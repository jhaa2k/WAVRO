package com.wavro.studio.app

import android.app.Application
import com.wavro.studio.editor.StudioViewModel
import com.wavro.studio.projects.ProjectRepository

class WavroApplication : Application() {
    lateinit var repository: ProjectRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = ProjectRepository(this)
    }

    companion object {
        lateinit var instance: WavroApplication
            private set
    }
}