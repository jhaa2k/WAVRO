package com.wavro.studio.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wavro.studio.audio.AudioEngine
import com.wavro.studio.audio.Track
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType
import com.wavro.studio.domain.StudioProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = AudioEngine(application)
    
    val playback = engine.state
    val fftData = engine.fftData

    private val _project = MutableStateFlow(StudioProject())
    val project: StateFlow<StudioProject> = _project.asStateFlow()

    fun playTrack(track: Track) = engine.playTrack(track)
    fun togglePlayback() = engine.togglePlayPause()
    fun seekTo(ms: Long) = engine.seekTo(ms)

    fun addLayer(type: LayerType) {
        val newLayer = Layer(id = UUID.randomUUID().toString(), type = type, name = type.name)
        _project.value = _project.value.copy(layers = _project.value.layers + newLayer)
    }

    fun updateLayer(updatedLayer: Layer) {
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
        )
    }

    fun deleteLayer(id: String) {
        _project.value = _project.value.copy(
            layers = _project.value.layers.filter { it.id != id }
        )
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}