package com.wavro.studio.app

import android.app.Application
import com.wavro.studio.projects.ProjectRepository

class WavroApplication : Application() {

    // Global reference to the repository so ViewModels can save/load projects
    lateinit var repository: ProjectRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ProjectRepository(this)
        instance = this
    }

    companion object {
        lateinit var instance: WavroApplication
            private set
    }
}