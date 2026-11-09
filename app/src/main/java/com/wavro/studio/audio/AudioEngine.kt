package com.wavro.studio.audio

import android.content.Context
import android.media.audiofx.Visualizer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioEngine(context: Context) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _fftData = MutableStateFlow(FloatArray(64))
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var visualizer: Visualizer? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startPositionTracker()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(durationMs = player.duration)
                    // Initialize visualizer ONLY when player is ready (fixes the 0 audioSessionId issue)
                    setupVisualizer() 
                }
            }
        })
    }

    fun playTrack(track: Track) {
        player.setMediaItem(MediaItem.fromUri(track.uri))
        player.prepare()
        player.play()
        _state.value = _state.value.copy(currentTrack = track, durationMs = track.duration)
    }

    private fun setupVisualizer() {
        visualizer?.release()
        val sessionId = player.audioSessionId
        if (sessionId == 0) return // Safety check

        try {
            @Suppress("DEPRECATION")
            visualizer = Visualizer(sessionId).apply {
                // Use a reasonable capture size
                captureSize = Visualizer.getCaptureSizeRange()[1] 
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, wf: ByteArray?, rate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                        if (fft != null && fft.isNotEmpty()) {
                            val bands = FloatArray(64)
                            val chunkSize = fft.size / 64
                            for (i in 0 until 64) {
                                var sum = 0f
                                for (j in 0 until chunkSize) {
                                    val index = (i * chunkSize + j) * 2
                                    if (index + 1 < fft.size) {
                                        val real = fft[index].toFloat()
                                        val imag = fft[index + 1].toFloat()
                                        sum += sqrt(real * real + imag * imag)
                                    }
                                }
                                // Normalize to 0.0 - 1.0
                                bands[i] = ((sum / chunkSize) / 128f).coerceIn(0f, 1f)
                            }
                            _fftData.value = bands
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPositionTracker() {
        scope.launch {
            while (player.isPlaying) {
                _state.value = _state.value.copy(positionMs = player.currentPosition)
                delay(100)
            }
        }
    }

    fun togglePlayPause() { if (player.isPlaying) player.pause() else player.play() }
    fun seekTo(ms: Long) { player.seekTo(ms); _state.value = _state.value.copy(positionMs = ms) }
    
    fun release() { 
        visualizer?.release()
        player.release() 
    }
}