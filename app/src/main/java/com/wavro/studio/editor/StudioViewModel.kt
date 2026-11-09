package com.wavro.studio.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wavro.studio.audio.AudioEngine
import com.wavro.studio.audio.MusicLibrary
import com.wavro.studio.audio.Track
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType
import com.wavro.studio.domain.StudioProject
import com.wavro.studio.rendering.VideoExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = AudioEngine(application)
    private val library = MusicLibrary(application)
    private val exporter = VideoExporter(application)
    
    val playback = engine.state
    val fftData = engine.fftData
    val eqBands = engine.eqBands
    val eqPresetName = engine.eqPresetName
    val bassBoostLevel = engine.bassBoostLevel
    val sleepTimerRemaining = engine.sleepTimerRemaining // Exposed here

    private val _libraryTracks = MutableStateFlow<List<Track>>(emptyList())
    val libraryTracks: StateFlow<List<Track>> = _libraryTracks.asStateFlow()

    private val _project = MutableStateFlow(StudioProject())
    val project: StateFlow<StudioProject> = _project.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()
    private val _exportStatus = MutableStateFlow("")
    val exportStatus: StateFlow<String> = _exportStatus.asStateFlow()

    init {
        viewModelScope.launch { 
            val tracks = library.scanDeviceForMusic()
            _libraryTracks.value = tracks
            engine.setPlaylist(tracks)
        }
    }

    fun playTrack(track: Track) = engine.playTrack(track)
    fun togglePlayback() = engine.togglePlayPause()
    fun seekTo(ms: Long) = engine.seekTo(ms)
    fun nextTrack() = engine.nextTrack()
    fun previousTrack() = engine.previousTrack()
    fun toggleShuffle() = engine.toggleShuffle()
    fun cycleRepeatMode() = engine.cycleRepeatMode()

    fun setEqBand(index: Short, level: Short) = engine.setEqBandLevel(index, level)
    fun applyEqPreset(presetName: String) = engine.applyEqPreset(presetName)
    fun setBassBoost(level: Int) = engine.setBassBoost(level)

    fun startSleepTimer(minutes: Int) = engine.startSleepTimer(minutes)
    fun cancelSleepTimer() = engine.cancelSleepTimer()

    fun selectLayer(id: String?) { _selectedLayerId.value = id }

    fun addLayer(type: LayerType) {
        val newLayer = Layer(id = UUID.randomUUID().toString(), type = type, name = type.name)
        _project.value = _project.value.copy(layers = _project.value.layers + newLayer)
        _selectedLayerId.value = newLayer.id
    }

    fun updateLayer(updatedLayer: Layer) {
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
        )
    }

    fun deleteLayer(id: String) {
        _project.value = _project.value.copy(layers = _project.value.layers.filter { it.id != id })
        if (_selectedLayerId.value == id) _selectedLayerId.value = null
    }

    fun exportVideo() {
        val currentTrack = playback.value.currentTrack ?: return
        if (_isExporting.value) return

        viewModelScope.launch {
            _isExporting.value = true
            _exportStatus.value = "Preparing..."
            _exportProgress.value = 0f
            try {
                val file = exporter.exportVideo(
                    audioUri = currentTrack.uri,
                    layers = _project.value.layers,
                    outputFileName = "WAVRO_Export_${System.currentTimeMillis()}.mp4"
                ) { progress ->
                    _exportProgress.value = progress
                    _exportStatus.value = "Exporting: ${(progress * 100).toInt()}%"
                }
                _exportStatus.value = "Saved: ${file.name}"
            } catch (e: Exception) {
                _exportStatus.value = "Error: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}