package com.wavro.studio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.wavro.studio.editor.StudioViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

private val WavroColors = darkColorScheme(
    primary = Color(0xFFFF9A6B),
    secondary = Color(0xFF8F7BFF),
    background = Color(0xFF0D0D12),
    surface = Color(0xFF17171E),
    surfaceVariant = Color(0xFF22222A),
    onBackground = Color(0xFFF4F1F0),
    onSurface = Color(0xFFF4F1F0),
)

@Composable
fun WavroApp(viewModel: StudioViewModel = viewModel()) {
    MaterialTheme(colorScheme = WavroColors) {
        StudioScreen(viewModel)
    }
}
