package com.wavro.studio.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wavro.studio.app.WavroApplication
import com.wavro.studio.editor.StudioViewModel

@Composable
fun WavroApp(app: WavroApplication) {
    val vm: StudioViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // Pass the Application to the ViewModel
            return StudioViewModel(app) as T
        }
    })

    // Pass ONLY the ViewModel to the screen
    StudioScreen(vm = vm)
}