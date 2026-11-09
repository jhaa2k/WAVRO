package com.wavro.studio.app

import android.app.Application
import android.content.Context
import com.wavro.studio.projects.ProjectRepository

class WavroApplication : Application() {

    // Expose the repository globally so the ViewModel can access it without passing context around
    lateinit var repository: ProjectRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize the Project Repository (for saving/loading visualizer projects)
        repository = ProjectRepository(this)
        
        // Keep a global instance for cases where we need context outside of Activities
        instance = this
    }

    companion object {
        lateinit var instance: WavroApplication
            private set
    }
}