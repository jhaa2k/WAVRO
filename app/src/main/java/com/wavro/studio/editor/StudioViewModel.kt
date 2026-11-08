package com.wavro.studio.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavro.studio.app.WavroApplication
import com.wavro.studio.audio.AudioEngine
import com.wavro.studio.audio.PlaybackState
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType
import com.wavro.studio.domain.StudioProject
import com.wavro.studio.visualization.SpectrumMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudioViewModel(private val app: WavroApplication) : ViewModel() {

    private val repo = app.repository
    private val engine = AudioEngine(app)

    private val _project = MutableStateFlow(repo.loadLast() ?: StudioProject())
    val project: StateFlow<StudioProject> = _project.asStateFlow()

    val playback: StateFlow<PlaybackState> = engine.state

    private val mapper = SpectrumMapper()
    private val _bands = MutableStateFlow(SpectrumMapper.Bands())
    val bands: StateFlow<SpectrumMapper.Bands> = _bands.asStateFlow()

    init {
        // Poll the visualizer at ~60Hz on a coroutine.
        viewModelScope.launch {
            while (true) {
                _bands.value = mapper.mapFft(engine.fftSnapshot())
                kotlinx.coroutines.delay(16)
            }
        }
    }

    fun loadTrack(uri: Uri, name: String?) {
        engine.load(uri, name)
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { l ->
                if (l.type == LayerType.BARS) l else l
            }
        )
    }

    fun togglePlayback() = engine.toggle()
    fun seekTo(ms: Long) = engine.seekTo(ms)

    fun addLayer(type: LayerType) {
        val layer = Layer(type = type, name = type.name.lowercase().replaceFirstChar { it.uppercase() })
        _project.value = _project.value.copy(layers = _project.value.layers + layer)
        persist()
    }

    fun updateLayer(layer: Layer) {
        _project.value = _project.value.copy(
            layers = _project.value.layers.map { if (it.id == layer.id) layer else it }
        )
        persist()
    }

    fun deleteLayer(id: String) {
        _project.value = _project.value.copy(layers = _project.value.layers.filter { it.id != id })
        persist()
    }

    private fun persist() {
        viewModelScope.launch { repo.save(_project.value.copy(lastModified = System.currentTimeMillis())) }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}