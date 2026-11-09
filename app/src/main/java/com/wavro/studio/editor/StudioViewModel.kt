package com.wavro.studio.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wavro.studio.audio.AudioEngine
import com.wavro.studio.audio.Track
import com.wavro.studio.domain.StudioProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = AudioEngine(application)
    
    // Expose playback state
    val playback = engine.state

    private val _project = MutableStateFlow(StudioProject())
    val project: StateFlow<StudioProject> = _project.asStateFlow()

    fun playTrack(track: Track) {
        engine.playTrack(track)
    }

    fun togglePlayback() {
        engine.togglePlayPause()
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}