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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = AudioEngine(application)
    private val library = MusicLibrary(application)
    
    val playback = engine.state
    val fftData = engine.fftData

    private val _project = MutableStateFlow(StudioProject())
    val project: StateFlow<StudioProject> = _project.asStateFlow()

    private val _libraryTracks = MutableStateFlow<List<Track>>(emptyList())
    val libraryTracks: StateFlow<List<Track>> = _libraryTracks.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    init {
        viewModelScope.launch { _libraryTracks.value = library.scanDeviceForMusic() }
    }

    fun playTrack(track: Track) = engine.playTrack(track)
    fun togglePlayback() = engine.togglePlayPause()
    fun seekTo(ms: Long) = engine.seekTo(ms)
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

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}