package com.wavro.studio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wavro.studio.app.WavroApplication
import com.wavro.studio.editor.StudioViewModel

@Composable
fun WavroApp(app: WavroApplication) {
    val vm: StudioViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            StudioViewModel(app) as T
    })

    val project by vm.project.collectAsState()
    val playback by vm.playback.collectAsState()
    val bands by vm.bands.collectAsState()

    StudioScreen(
        vm = vm,
        projectState = vm.project,
        playback = vm.playback,
        bands = vm.bands
    )
}