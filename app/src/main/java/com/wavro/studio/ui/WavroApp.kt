package com.wavro.studio.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wavro.studio.app.WavroApplication
import com.wavro.studio.editor.StudioViewModel

@Composable
fun WavroApp(app: WavroApplication) {
    // Initialize the ViewModel and pass the Application context to it
    val vm: StudioViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudioViewModel(app) as T
        }
    })

    // Launch the main UI screen
    StudioScreen(vm = vm)
}