package com.wavro.studio.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.wavro.studio.audio.AudioEngine
import com.wavro.studio.audio.PlaybackState
import com.wavro.studio.domain.ElementType
import com.wavro.studio.domain.ProjectElement
import com.wavro.studio.domain.StudioProject
import com.wavro.studio.projects.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val audioEngine = AudioEngine(application)
    private val repository = ProjectRepository(application)

    private val mutableProject = MutableStateFlow(repository.current())
    val project: StateFlow<StudioProject> = mutableProject.asStateFlow()
    val playback: StateFlow<PlaybackState> = audioEngine.state

    private val mutableSavedProjects = MutableStateFlow(repository.all())
    val savedProjects: StateFlow<List<StudioProject>> = mutableSavedProjects.asStateFlow()

    fun openAudio(uri: Uri) = audioEngine.open(uri)
    fun startDemo() = audioEngine.startDemo()
    fun togglePlayback() = audioEngine.togglePlayback()
    fun seekTo(positionMs: Long) = audioEngine.seekTo(positionMs)
    fun refreshVisualization() = audioEngine.refreshVisualization()

    fun updateProjectName(name: String) {
        mutableProject.value = mutableProject.value.copy(name = name)
    }

    fun saveProject() {
        repository.save(mutableProject.value)
        refreshProjects()
    }

    fun newProject() {
        val project = StudioProject(name = "Untitled Project ${mutableSavedProjects.value.size + 1}")
        mutableProject.value = project
        repository.save(project)
        refreshProjects()
    }

    fun selectProject(project: StudioProject) {
        mutableProject.value = project
        repository.select(project)
    }

    fun deleteProject(projectId: String) {
        repository.delete(projectId)
        if (mutableProject.value.id == projectId) {
            val next = repository.all().firstOrNull() ?: StudioProject()
            mutableProject.value = next
            repository.save(next)
        }
        refreshProjects()
    }

    fun addElement(type: ElementType) {
        val element = ProjectElement(type = type)
        mutableProject.value = mutableProject.value.copy(
            elements = mutableProject.value.elements + element,
        )
    }

    fun setElementEnabled(elementId: String, enabled: Boolean) {
        mutableProject.value = mutableProject.value.copy(
            elements = mutableProject.value.elements.map {
                if (it.id == elementId) it.copy(enabled = enabled) else it
            },
        )
    }

    fun removeElement(elementId: String) {
        mutableProject.value = mutableProject.value.copy(
            elements = mutableProject.value.elements.filterNot { it.id == elementId },
        )
    }

    private fun refreshProjects() {
        mutableSavedProjects.value = repository.all()
    }

    override fun onCleared() {
        audioEngine.release()
        super.onCleared()
    }
}
