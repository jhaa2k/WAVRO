package com.wavro.studio.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioEngine(context: Context) {
    // --- State ---
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _fftData = MutableStateFlow(FloatArray(64))
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    private val _eqBands = MutableStateFlow<List<Short>>(emptyList())
    val eqBands: StateFlow<List<Short>> = _eqBands.asStateFlow()

    private val _eqPresetName = MutableStateFlow("Custom")
    val eqPresetName: StateFlow<String> = _eqPresetName.asStateFlow()

    private val _bassBoostLevel = MutableStateFlow(0)
    val bassBoostLevel: StateFlow<Int> = _bassBoostLevel.asStateFlow()

    // --- Core ---
    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // --- Effects ---
    private var visualizer: Visualizer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    // --- Playback Queue & Modes ---
    private var playlist: List<Track> = emptyList()
    private var currentIndex = -1
    private var isShuffleOn = false
    private var repeatMode = Player.REPEAT_MODE_OFF

    // --- Sleep Timer ---
    private var sleepTimerJob: Job? = null

    init {
        setupAudioFocus()
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startPositionTracker() else stopVisualizer()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(durationMs = player.duration)
                    setupVisualizer()
                    setupEffects()
                } else if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Sync current index when track changes automatically
                val newIndex = mediaItem?.mediaId?.toIntOrNull() ?: -1
                if (newIndex != -1) currentIndex = newIndex
            }
        })
    }

    // ==========================================
    // 1. PLAYBACK CONTROLS & QUEUE
    // ==========================================

    fun setPlaylist(tracks: List<Track>) {
        this.playlist = tracks
    }

    fun playTrack(track: Track) {
        val idx = playlist.indexOfFirst { it.uri == track.uri }
        if (idx != -1) currentIndex = idx
        startPlayback(track)
    }

    private fun startPlayback(track: Track) {
        player.setMediaItem(MediaItem.Builder().setUri(track.uri).setMediaId(currentIndex.toString()).build())
        player.prepare()
        player.play()
        _state.value = _state.value.copy(currentTrack = track, durationMs = track.duration)
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        _state.value = _state.value.copy(positionMs = ms)
    }

    fun nextTrack() {
        if (playlist.isEmpty()) return
        if (isShuffleOn) {
            currentIndex = playlist.indices.random()
        } else {
            currentIndex++
            if (currentIndex >= playlist.size) {
                currentIndex = if (repeatMode == Player.REPEAT_MODE_ALL) 0 else playlist.size - 1
            }
        }
        startPlayback(playlist[currentIndex])
    }

    fun previousTrack() {
        if (playlist.isEmpty()) return
        if (isShuffleOn) {
            currentIndex = playlist.indices.random()
        } else {
            currentIndex--
            if (currentIndex < 0) {
                currentIndex = if (repeatMode == Player.REPEAT_MODE_ALL) playlist.size - 1 else 0
            }
        }
        startPlayback(playlist[currentIndex])
    }

    fun toggleShuffle() {
        isShuffleOn = !isShuffleOn
        player.shuffleModeEnabled = isShuffleOn
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = repeatMode
    }

    private fun handleTrackEnded() {
        if (repeatMode == Player.REPEAT_MODE_ONE) {
            player.seekTo(0)
            player.play()
        } else {
            nextTrack()
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

    // ==========================================
    // 2. AUDIO FOCUS (Background Playback)
    // ==========================================

    private fun setupAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> player.pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
                        AudioManager.AUDIOFOCUS_GAIN -> player.play()
                    }
                }.build()
        }
    }

    // ==========================================
    // 3. HARDWARE EFFECTS (EQ & BASS BOOST)
    // ==========================================

    private fun setupEffects() {
        equalizer?.release()
        bassBoost?.release()
        try {
            val sessionId = player.audioSessionId
            if (sessionId != 0) {
                equalizer = Equalizer(0, sessionId).apply { enabled = true }
                bassBoost = BassBoost(0, sessionId).apply { 
                    enabled = true 
                    setStrength(0.toShort())
                }
                
                // Read bands into state
                val bands = mutableListOf<Short>()
                for (i in 0 until equalizer!!.numberOfBands) {
                    bands.add(equalizer!!.getBandLevel(i.toShort()))
                }
                _eqBands.value = bands
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun setEqBandLevel(bandIndex: Short, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, level)
            val current = _eqBands.value.toMutableList()
            if (bandIndex.toInt() < current.size) {
                current[bandIndex.toInt()] = level
                _eqBands.value = current
                _eqPresetName.value = "Custom"
            }
        } catch (e: Exception) {}
    }

    fun setBassBoost(level: Int) { // 0 to 1000
        try {
            bassBoost?.setStrength(level.toShort())
            _bassBoostLevel.value = level
        } catch (e: Exception) {}
    }

    fun applyEqPreset(presetName: String) {
        val eq = equalizer ?: return
        try {
            // Android Equalizer presets are indexed 0 to numberOfPresets - 1
            val numPresets = eq.numberOfPresets
            for (i in 0 until numPresets) {
                val presetNameString = eq.getPresetName(i.toShort())
                if (presetNameString.equals(presetName, ignoreCase = true)) {
                    eq.usePreset(i.toShort())
                    _eqPresetName.value = presetName
                    // Update state bands
                    val bands = mutableListOf<Short>()
                    for (j in 0 until eq.numberOfBands) {
                        bands.add(eq.getBandLevel(j.toShort()))
                    }
                    _eqBands.value = bands
                    return
                }
            }
        } catch (e: Exception) {}
    }

    // ==========================================
    // 4. FFT VISUALIZER
    // ==========================================

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

    private fun stopVisualizer() {
        visualizer?.enabled = false
    }

    // ==========================================
    // 5. SLEEP TIMER
    // ==========================================

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = scope.launch {
            delay(minutes * 60 * 1000L)
            player.pause() // Auto-pause when timer finishes
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
    }

    // ==========================================
    // 6. CLEANUP
    // ==========================================

    fun release() {
        sleepTimerJob?.cancel()
        visualizer?.release()
        equalizer?.release()
        bassBoost?.release()
        player.release()
    }
}