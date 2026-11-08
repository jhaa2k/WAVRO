package com.wavro.studio.audio

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import com.wavro.studio.visualization.SpectrumMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    private var player: MediaPlayer? = null
    private var demoTrack: AudioTrack? = null
    private var visualizer: Visualizer? = null
    private var demoStartedAt = 0L

    private val ticker = object : Runnable {
        override fun run() {
            val current = mutableState.value
            if (!current.isPlaying) return
            val position = if (current.isDemo) {
                ((System.currentTimeMillis() - demoStartedAt) % DEMO_DURATION_MS)
            } else {
                player?.currentPosition?.toLong() ?: current.positionMs
            }
            val spectrum = if (current.isDemo && !current.visualizationAvailable) {
                demoSpectrum(System.currentTimeMillis())
            } else {
                current.spectrum
            }
            mutableState.value = current.copy(positionMs = position, spectrum = spectrum)
            mainHandler.postDelayed(this, TICK_MS)
        }
    }

    fun open(uri: Uri) {
        stopSources()
        val title = displayName(appContext.contentResolver, uri)
        mutableState.value = PlaybackState(title = title, artist = "Local audio file", isLoading = true)
        try {
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
            )
            mediaPlayer.setDataSource(appContext, uri)
            mediaPlayer.setOnPreparedListener { prepared ->
                prepared.start()
                mutableState.value = mutableState.value.copy(
                    isPlaying = true,
                    isLoading = false,
                    durationMs = prepared.duration.toLong(),
                    error = null,
                )
                attachVisualizer(prepared.audioSessionId)
                mainHandler.post(ticker)
            }
            mediaPlayer.setOnCompletionListener {
                releaseVisualizer()
                mutableState.value = mutableState.value.copy(isPlaying = false, isLoading = false, positionMs = 0)
                mainHandler.removeCallbacks(ticker)
            }
            mediaPlayer.setOnErrorListener { failedPlayer, what, extra ->
                val message = "Unable to play this audio file ($what/$extra)"
                Log.e(TAG, message)
                failedPlayer.release()
                if (player === failedPlayer) player = null
                releaseVisualizer()
                mutableState.value = mutableState.value.copy(
                    isPlaying = false,
                    isLoading = false,
                    error = message,
                )
                true
            }
            mediaPlayer.prepareAsync()
        } catch (error: SecurityException) {
            failToOpen(error)
        } catch (error: IllegalArgumentException) {
            failToOpen(error)
        } catch (error: java.io.IOException) {
            failToOpen(error)
        } catch (error: IllegalStateException) {
            failToOpen(error)
        }
    }

    fun startDemo() {
        stopSources()
        try {
            val samples = makeDemoSamples()
            val format = AudioFormat.Builder()
                .setSampleRate(DEMO_SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(format)
                .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            val written = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            if (written != samples.size) {
                track.release()
                error("Could not load the demo audio track")
            }
            track.setLoopPoints(0, samples.size, -1)
            track.setVolume(0.75f)
            demoTrack = track
            demoStartedAt = System.currentTimeMillis()
            track.play()
            mutableState.value = PlaybackState(
                title = "WAVRO Demo",
                artist = "Built-in audio generator",
                isPlaying = true,
                isLoading = false,
                isDemo = true,
                durationMs = DEMO_DURATION_MS,
            )
            attachVisualizer(track.audioSessionId)
            mainHandler.post(ticker)
        } catch (error: IllegalArgumentException) {
            failToOpen(error)
        } catch (error: IllegalStateException) {
            failToOpen(error)
        }
    }

    fun togglePlayback() {
        val current = mutableState.value
        if (current.isLoading) return
        if (current.isDemo) {
            val track = demoTrack ?: return
            if (current.isPlaying) {
                track.pause()
                mainHandler.removeCallbacks(ticker)
                mutableState.value = current.copy(isPlaying = false)
            } else {
                demoStartedAt = System.currentTimeMillis() - current.positionMs
                track.play()
                mutableState.value = current.copy(isPlaying = true)
                mainHandler.post(ticker)
            }
            return
        }

        val mediaPlayer = player ?: return
        try {
            if (current.isPlaying) {
                mediaPlayer.pause()
                mainHandler.removeCallbacks(ticker)
                mutableState.value = current.copy(isPlaying = false)
            } else {
                mediaPlayer.start()
                mutableState.value = current.copy(isPlaying = true, error = null)
                mainHandler.post(ticker)
            }
        } catch (error: IllegalStateException) {
            failToOpen(error)
        }
    }

    fun seekTo(positionMs: Long) {
        val current = mutableState.value
        val position = positionMs.coerceIn(0, current.durationMs.coerceAtLeast(0))
        if (current.isDemo) {
            demoStartedAt = System.currentTimeMillis() - position
            val track = demoTrack
            if (track != null) {
                val frame = ((position % DEMO_DURATION_MS) * DEMO_SAMPLE_RATE / 1000).toInt()
                val wasPlaying = current.isPlaying
                track.pause()
                track.setPlaybackHeadPosition(frame)
                if (wasPlaying) track.play()
            }
            mutableState.value = current.copy(positionMs = position)
        } else {
            try {
                player?.seekTo(position.toInt())
                mutableState.value = current.copy(positionMs = position)
            } catch (error: IllegalStateException) {
                failToOpen(error)
            }
        }
    }

    fun refreshVisualization() {
        val sessionId = player?.audioSessionId ?: demoTrack?.audioSessionId ?: return
        attachVisualizer(sessionId)
    }

    fun release() {
        stopSources()
    }

    private fun attachVisualizer(sessionId: Int) {
        releaseVisualizer()
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mutableState.value = mutableState.value.copy(visualizationAvailable = false)
            return
        }
        try {
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            val effect = Visualizer(sessionId).apply {
                setCaptureSize(captureSize)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int,
                        ) {
                            if (fft != null) {
                                mutableState.value = mutableState.value.copy(
                                    spectrum = SpectrumMapper.fromFft(fft),
                                    visualizationAvailable = true,
                                )
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true,
                )
                enabled = true
            }
            visualizer = effect
            mutableState.value = mutableState.value.copy(visualizationAvailable = true, error = null)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Audio visualization could not be started", error)
            mutableState.value = mutableState.value.copy(
                visualizationAvailable = false,
                error = "Audio is playing, but live visualization is unavailable on this device.",
            )
        }
    }

    private fun releaseVisualizer() {
        visualizer?.let { effect ->
            try {
                effect.enabled = false
                effect.release()
            } catch (error: IllegalStateException) {
                Log.w(TAG, "Audio visualizer was already released", error)
            }
        }
        visualizer = null
    }

    private fun stopSources() {
        mainHandler.removeCallbacks(ticker)
        releaseVisualizer()
        player?.let {
            it.setOnPreparedListener(null)
            it.setOnCompletionListener(null)
            it.setOnErrorListener(null)
            it.release()
        }
        player = null
        demoTrack?.let {
            it.pause()
            it.flush()
            it.release()
        }
        demoTrack = null
    }

    private fun failToOpen(error: Exception) {
        Log.e(TAG, "Audio source could not be opened", error)
        stopSources()
        mutableState.value = mutableState.value.copy(
            isPlaying = false,
            isLoading = false,
            error = error.message ?: "Audio source could not be opened.",
        )
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String {
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
            uri.lastPathSegment?.substringAfterLast('/') ?: "Audio file"
        } catch (error: SecurityException) {
            Log.w(TAG, "Audio display name is unavailable", error)
            "Audio file"
        }
    }

    private fun demoSpectrum(nowMs: Long): List<Float> {
        val phase = nowMs / 1000.0
        return List(PlaybackState.SPECTRUM_BINS) { index ->
            val bass = 0.5 + 0.5 * sin(phase * 2.1)
            val peakA = exp(-((index - (5 + 3 * sin(phase * 0.65))) / 4.0).let { it * it })
            val peakB = exp(-((index - (21 + 7 * sin(phase * 0.38))) / 6.0).let { it * it })
            (0.08 + peakA * bass * 0.78 + peakB * 0.42).toFloat().coerceIn(0f, 1f)
        }
    }

    private fun makeDemoSamples(): ShortArray {
        val sampleCount = DEMO_SAMPLE_RATE * (DEMO_DURATION_MS / 1000).toInt()
        return ShortArray(sampleCount) { index ->
            val time = index.toDouble() / DEMO_SAMPLE_RATE
            val beatPhase = time % 0.5
            val kickEnvelope = if (beatPhase < 0.2) exp(-beatPhase * 22.0) else 0.0
            val kick = sin(2.0 * PI * (48.0 + 34.0 * exp(-beatPhase * 18.0)) * time) * kickEnvelope * 0.48
            val note = (time / 2.0).toInt() % DEMO_NOTES.size
            val bass = sin(2.0 * PI * DEMO_NOTES[note] * time) * 0.18
            val chord = sin(2.0 * PI * (DEMO_NOTES[note] * 1.5) * time) * 0.08
            val shimmer = sin(2.0 * PI * 1200.0 * time) * if (beatPhase < 0.035) 0.025 else 0.0
            ((kick + bass + chord + shimmer).coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private companion object {
        const val TAG = "AudioEngine"
        const val TICK_MS = 80L
        const val DEMO_SAMPLE_RATE = 22_050
        const val DEMO_DURATION_MS = 8_000L
        val DEMO_NOTES = doubleArrayOf(55.0, 65.41, 73.42, 49.0)
    }
}
