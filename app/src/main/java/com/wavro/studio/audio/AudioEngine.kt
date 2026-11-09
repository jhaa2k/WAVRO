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

class AudioEngine(context: Context) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _fftData = MutableStateFlow(FloatArray(64))
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var visualizer: Visualizer? = null
    private val nativeEngine = NativeAudioEngine() // C++ Engine

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startPositionTracker()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(durationMs = player.duration)
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
        if (player.audioSessionId == 0) return

        try {
            @Suppress("DEPRECATION")
            visualizer = Visualizer(player.audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, wf: ByteArray?, rate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                        if (fft != null && fft.isNotEmpty()) {
                            val bands = FloatArray(64)
                            // Call C++ Native function for ultra-fast FFT
                            nativeEngine.calculateFFT(fft, bands)
                            _fftData.value = bands
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
        } catch (e: Exception) { e.printStackTrace() }
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
    fun release() { visualizer?.release(); player.release() }
}