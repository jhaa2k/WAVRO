package com.wavro.studio.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin

class VideoExporter(private val context: Context) {

    private val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private val TIMEOUT_US = 10000L
    private val FRAME_RATE = 30
    private val VIDEO_WIDTH = 1080
    private val VIDEO_HEIGHT = 1920

    suspend fun exportVideo(
        audioUri: Uri,
        layers: List<Layer>,
        outputFileName: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.Default) {
        
        // 1. Create Output File
        val outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), outputFileName)
        
        // 2. Extract Audio Info
        val extractor = MediaExtractor()
        context.contentResolver.openFileDescriptor(audioUri, "r")?.use { pfd ->
            extractor.setDataSource(pfd.fileDescriptor)
        }
        
        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }
        
        if (audioFormat == null) throw IOException("No audio track found in file")
        
        val durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION)

        // 3. Setup Video Encoder
        val videoFormat = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 6000000) // 6 Mbps
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val videoCodec = MediaCodec.createEncoderByType(MIME_TYPE)
        videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoCodec.start()

        // 4. Setup Muxer
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        
        val totalFrames = (durationUs / 1_000_000f * FRAME_RATE).toInt()
        val bufferInfo = MediaCodec.BufferInfo()
        var muxerStarted = false
        var videoTrackIndex = -1

        // Bitmap for rendering frames
        val bitmap = Bitmap.createBitmap(VIDEO_WIDTH, VIDEO_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 5. Render Loop
        for (frame in 0 until totalFrames) {
            val timeUs = frame * (1_000_000L / FRAME_RATE)
            
            // A. Draw the frame
            drawFrame(canvas, layers, frame)

            // B. Feed to encoder
            val inputBufferIndex = videoCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputImage = videoCodec.getInputImage(inputBufferIndex)
                if (inputImage != null) {
                    val yuv = bitmapToYuv(bitmap)
                    inputImage.planes[0].buffer.put(yuv[0])
                    inputImage.planes[1].buffer.put(yuv[1])
                    inputImage.planes[2].buffer.put(yuv[2])
                }
                videoCodec.queueInputBuffer(inputBufferIndex, 0, 0, timeUs, 0)
            }

            // C. Drain Encoder
            var encoderStatus = videoCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            while (encoderStatus >= 0) {
                val encodedData = videoCodec.getOutputBuffer(encoderStatus)
                if (encodedData != null && bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        videoTrackIndex = muxer.addTrack(videoCodec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                }
                videoCodec.releaseOutputBuffer(encoderStatus, false)
                encoderStatus = videoCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            }
            
            onProgress(frame.toFloat() / totalFrames)
        }

        videoCodec.signalEndOfInputStream()

        // 6. Merge Audio Track
        if (audioFormat != null) {
            val audioTrackIndexOut = muxer.addTrack(audioFormat)
            extractor.selectTrack(audioTrackIndex)
            val maxInputSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            val inputBuffer = ByteBuffer.allocateDirect(maxInputSize)
            val audioBufferInfo = MediaCodec.BufferInfo()
            
            while (true) {
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) break
                
                audioBufferInfo.offset = 0
                audioBufferInfo.size = sampleSize
                audioBufferInfo.presentationTimeUs = extractor.sampleTime
                audioBufferInfo.flags = extractor.sampleFlags
                
                muxer.writeSampleData(audioTrackIndexOut, inputBuffer, audioBufferInfo)
                extractor.advance()
            }
        }

        // 7. Cleanup
        extractor.release()
        if (muxerStarted) muxer.stop()
        muxer.release()
        videoCodec.stop()
        videoCodec.release()

        return@withContext outputFile
    }

    // ==========================================
    // DRAWING LOGIC (Renders Layers to Canvas)
    // ==========================================
    private fun drawFrame(canvas: Canvas, layers: List<Layer>, frame: Int) {
        canvas.drawColor(Color.BLACK)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Simulate audio data for the export
        val fakeFft = FloatArray(64)
        for (i in 0 until 64) {
            val wave = sin(frame * 0.1f + i * 0.5f) * cos(frame * 0.05f)
            fakeFft[i] = (wave * 0.5f + 0.5f).coerceIn(0f, 1f)
        }

        layers.filter { it.visible }.forEach { layer ->
            canvas.save()
            
            // Apply Transformations
            val cx = (layer.x / 100f) * VIDEO_WIDTH
            val cy = (layer.y / 100f) * VIDEO_HEIGHT
            canvas.rotate(layer.rotation, cx, cy)
            canvas.scale(layer.scale / 100f, layer.scale / 100f, cx, cy)
            
            paint.color = layer.colorPrimary
            paint.alpha = (layer.opacity / 100f * 255).toInt()

            when (layer.type) {
                LayerType.BARS -> {
                    val barCount = layer.spectrum.barCount
                    val totalWidth = VIDEO_WIDTH * 0.8f
                    val spacing = totalWidth / barCount
                    val barWidth = spacing * 0.7f
                    val maxHeight = VIDEO_HEIGHT * 0.3f
                    val startX = cx - (totalWidth / 2f)

                    for (i in 0 until barCount) {
                        val fftIndex = (i.toFloat() / barCount * fakeFft.size).toInt().coerceIn(0, fakeFft.size - 1)
                        val amplitude = (fakeFft[fftIndex] * layer.spectrum.sensitivity).coerceIn(0.01f, 1f)
                        val barHeight = (maxHeight * amplitude).coerceAtLeast(4f)

                        val rect = RectF(
                            startX + (i * spacing), 
                            cy - barHeight, 
                            startX + (i * spacing) + barWidth, 
                            cy
                        )
                        canvas.drawRect(rect, paint)
                    }
                }
                LayerType.CIRCLE -> {
                    val barCount = layer.spectrum.barCount
                    val radius = VIDEO_WIDTH * 0.15f
                    val maxBarLength = VIDEO_WIDTH * 0.2f
                    val strokePaint = Paint(paint).apply { strokeWidth = 6f; style = Paint.Style.STROKE }

                    for (i in 0 until barCount) {
                        val fftIndex = (i.toFloat() / barCount * fakeFft.size).toInt().coerceIn(0, fakeFft.size - 1)
                        val amplitude = (fakeFft[fftIndex] * layer.spectrum.sensitivity).coerceIn(0.01f, 1f)
                        val barLength = (maxBarLength * amplitude).coerceAtLeast(4f)
                        
                        val angle = (i.toFloat() / barCount) * (Math.PI * 2).toFloat() - (Math.PI / 2).toFloat()
                        val startX = cx + cos(angle) * radius
                        val startY = cy + sin(angle) * radius
                        val endX = cx + cos(angle) * (radius + barLength)
                        val endY = cy + sin(angle) * (radius + barLength)
                        
                        canvas.drawLine(startX, startY, endX, endY, strokePaint)
                    }
                }
                LayerType.TEXT -> {
                    val textPaint = Paint(paint).apply {
                        textSize = layer.text.textSize * 2f // Scaled up for video resolution
                        textAlign = Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT, 
                            if (layer.text.isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                        )
                    }
                    canvas.drawText(layer.text.text, cx, cy, textPaint)
                }
                else -> Unit
            }
            canvas.restore()
        }
    }

    // ==========================================
    // BITMAP TO YUV CONVERTER (Required for Video Encoder)
    // ==========================================
    private fun bitmapToYuv(bitmap: Bitmap): Array<ByteBuffer> {
        val width = bitmap.width
        val height = bitmap.height
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        
        val yuv = arrayOf(
            ByteBuffer.allocateDirect(width * height),
            ByteBuffer.allocateDirect(width * height / 4),
            ByteBuffer.allocateDirect(width * height / 4)
        )
        
        val y = yuv[0]
        val u = yuv[1]
        val v = yuv[2]
        
        for (j in 0 until height) {
            for (i in 0 until width) {
                val color = argb[j * width + i]
                val r = (color shr 16) and 0xff
                val g = (color shr 8) and 0xff
                val b = color and 0xff
                
                // YUV conversion math
                val yValue = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                y.put(yValue.toByte())
                
                if (j % 2 == 0 && i % 2 == 0) {
                    val uValue = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                    val vValue = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                    u.put(uValue.toByte())
                    v.put(vValue.toByte())
                }
            }
        }
        y.rewind(); u.rewind(); v.rewind()
        return yuv
    }
}