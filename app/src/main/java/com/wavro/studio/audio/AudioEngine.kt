package com.wavro.studio.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEngine(context: Context) {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(durationMs = duration)
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.value = _state.value.copy(error = error.message)
            }
        })
        setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(androidx.media3.common.C.audioContentType.MUSIC)
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .build(),
            true
        )
    }

    private var visualizer: Visualizer? = null
    private val fftBuffer = ByteArray(1024)

    fun load(uri: Uri, trackName: String? = null) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        _state.value = _state.value.copy(currentTrackName = trackName)
        attachVisualizer()
    }

    private fun attachVisualizer() {
        visualizer?.release()
        try {
            @Suppress("DEPRECATION")
            visualizer = Visualizer(player.audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0].coerceAtMost(1024)
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, wf: ByteArray?, rate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                        if (fft != null) fft.copyInto(fftBuffer, 0, 0, minOf(fft.size, fftBuffer.size))
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Visualizer unavailable: ${e.message}")
        }
    }

    fun play() { player.play() }
    fun pause() { player.pause() }
    fun toggle() { if (player.isPlaying) pause() else play() }
    fun seekTo(ms: Long) { player.seekTo(ms) }
    fun setSpeed(speed: Float) { player.setPlaybackSpeed(speed) }

    fun fftSnapshot(): ByteArray = fftBuffer.copyOf()

    fun release() {
        visualizer?.release()
        visualizer = null
        player.release()
    }
}