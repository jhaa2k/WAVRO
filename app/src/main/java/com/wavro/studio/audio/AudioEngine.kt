package com.wavro.studio.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
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
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    // Equalizer State
    private val _eqBands = MutableStateFlow<List<Short>>(emptyList())
    val eqBands: StateFlow<List<Short>> = _eqBands.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startPositionTracker() else stopVisualizer()
            }
        })
    }

    fun playTrack(track: Track) {
        player.setMediaItem(MediaItem.fromUri(track.uri))
        player.prepare()
        player.play()
        _state.value = _state.value.copy(currentTrack = track, durationMs = track.duration)
        
        // Initialize Audio Effects
        setupVisualizer()
        setupEqualizer()
    }

    private fun setupEqualizer() {
        equalizer?.release()
        bassBoost?.release()
        try {
            val sessionId = player.audioSessionId
            if (sessionId != 0) {
                equalizer = Equalizer(0, sessionId).apply { enabled = true }
                bassBoost = BassBoost(0, sessionId).apply { 
                    enabled = true 
                    setStrength(500.toShort()) // 0 to 1000
                }
                
                // Load available bands into State
                val bands = mutableListOf<Short>()
                for (i in 0 until equalizer!!.numberOfBands) {
                    bands.add(equalizer!!.getBandLevel(i))
                }
                _eqBands.value = bands
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEqBandLevel(bandIndex: Short, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, level)
            val current = _eqBands.value.toMutableList()
            if (bandIndex.toInt() < current.size) {
                current[bandIndex.toInt()] = level
                _eqBands.value = current
            }
        } catch (e: Exception) {}
    }

    // --- Visualizer Logic ---
    private fun setupVisualizer() {
        visualizer?.release()
        try {
            @Suppress("DEPRECATION")
            visualizer = Visualizer(player.audioSessionId).apply {
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
                                bands[i] = ((sum / chunkSize) / 128f).coerceIn(0f, 1f)
                            }
                            _fftData.value = bands
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
        } catch (e: Exception) {}
    }

    private fun stopVisualizer() { visualizer?.enabled = false }

    private fun startPositionTracker() {
        scope.launch {
            while (player.isPlaying) {
                _state.value = _state.value.copy(
                    positionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0)
                )
                delay(200)
            }
        }
    }

    fun togglePlayPause() { if (player.isPlaying) player.pause() else player.play() }
    fun seekTo(ms: Long) { player.seekTo(ms); _state.value = _state.value.copy(positionMs = ms) }
    
    fun release() { 
        visualizer?.release()
        equalizer?.release()
        bassBoost?.release()
        player.release() 
    }
}