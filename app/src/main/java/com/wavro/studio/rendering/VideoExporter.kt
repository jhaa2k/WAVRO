package com.wavro.studio.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.view.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
    private val IFRAME_INTERVAL = 1

    // We use a bitmap renderer for simplicity in this example
    suspend fun exportVideo(
        audioUri: Uri,
        layers: List<Layer>,
        outputFileName: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.Default) {
        
        val outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), outputFileName)
        
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
        
        if (audioFormat == null) throw IOException("No audio track found")
        
        val durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION)
        val width = 1080
        val height = 1920

        val videoFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 6000000)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        }

        val videoCodec = MediaCodec.createEncoderByType(MIME_TYPE)
        videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoCodec.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        
        val totalFrames = (durationUs / 1_000_000f * FRAME_RATE).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val density = Density(context)
        val drawScope = CanvasDrawScope()
        val bufferInfo = MediaCodec.BufferInfo()
        var muxerStarted = false
        var videoTrackIndex = -1
        val fakeFft = FloatArray(64)

        for (frame in 0 until totalFrames) {
            val timeUs = frame * (1_000_000L / FRAME_RATE)
            
            for (i in 0 until 64) {
                val wave = sin(frame * 0.1f + i * 0.5f) * cos(frame * 0.05f)
                fakeFft[i] = (wave * 0.5f + 0.5f).coerceIn(0f, 1f)
            }

            canvas.drawColor(Color.BLACK)
            drawScope.draw(density, LayoutDirection.Ltr, canvas, Size(width.toFloat(), height.toFloat())) {
                layers.forEach { layer ->
                    val cx = size.width * (layer.x / 100f)
                    val cy = size.height * (layer.y / 100f)
                    scale(layer.scale / 100f, pivot = Offset(cx, cy)) {
                        when (layer.type) {
                            LayerType.BARS -> drawSpectrumBars(layer, fakeFft, cx, cy)
                            LayerType.CIRCLE -> drawCircularSpectrum(layer, fakeFft, cx, cy)
                            LayerType.TEXT -> drawText(layer, cx, cy)
                            else -> Unit
                        }
                    }
                }
            }

            val inputBufferIndex = videoCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputImage = videoCodec.getInputImage(inputBufferIndex)
                if (inputImage != null) {
                    // Convert Bitmap to YUV (Simplified for chat, real production uses OpenGL)
                    val yuv = bitmapToYuv(bitmap)
                    inputImage.planes[0].buffer.put(yuv[0])
                    inputImage.planes[1].buffer.put(yuv[1])
                    inputImage.planes[2].buffer.put(yuv[2])
                }
                videoCodec.queueInputBuffer(inputBufferIndex, 0, 0, timeUs, 0)
            }

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

        extractor.release()
        if (muxerStarted) muxer.stop()
        muxer.release()
        videoCodec.stop()
        videoCodec.release()

        return@withContext outputFile
    }

    // Simple bitmap to YUV conversion
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

    private fun DrawScope.drawSpectrumBars(layer: Layer, fft: FloatArray, cx: Float, cy: Float) {
        val barCount = layer.spectrum.barCount
        val totalWidth = size.width * 0.8f
        val spacing = totalWidth / barCount
        val barWidth = spacing * 0.7f
        val maxHeight = size.height * 0.3f
        val startX = cx - (totalWidth / 2f)

        for (i in 0 until barCount) {
            val fftIndex = (i.toFloat() / barCount * fft.size).toInt().coerceIn(0, fft.size - 1)
            val amplitude = (fft[fftIndex] * layer.spectrum.sensitivity).coerceIn(0.01f, 1f)
            val barHeight = (maxHeight * amplitude).coerceAtLeast(4f)
            drawRect(
                color = ComposeColor(layer.colorPrimary),
                topLeft = Offset(startX + (i * spacing), cy - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }

    private fun DrawScope.drawCircularSpectrum(layer: Layer, fft: FloatArray, cx: Float, cy: Float) {
        val barCount = layer.spectrum.barCount
        val radius = size.minDimension * 0.15f
        val maxBarLength = size.minDimension * 0.2f

        for (i in 0 until barCount) {
            val fftIndex = (i.toFloat() / barCount * fft.size).toInt().coerceIn(0, fft.size - 1)
            val amplitude = (fft[fftIndex] * layer.spectrum.sensitivity).coerceIn(0.01f, 1f)
            val barLength = (maxBarLength * amplitude).coerceAtLeast(4f)
            val angle = (i.toFloat() / barCount) * (Math.PI * 2).toFloat() - (Math.PI / 2).toFloat()
            val startX = cx + cos(angle) * radius
            val startY = cy + sin(angle) * radius
            val endX = cx + cos(angle) * (radius + barLength)
            val endY = cy + sin(angle) * (radius + barLength)
            drawLine(color = ComposeColor(layer.colorPrimary), start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 6f)
        }
    }

    private fun DrawScope.drawText(layer: Layer, cx: Float, cy: Float) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = Paint().apply {
                color = layer.colorPrimary
                textSize = layer.text.textSize * density * 2f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (layer.text.isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }
            drawText(layer.text.text, cx, cy, paint)
        }
    }
}