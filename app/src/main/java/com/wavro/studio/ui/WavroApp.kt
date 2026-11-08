package com.wavro.studio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wavro.studio.editor.StudioViewModel

private val AudiovisColors = darkColorScheme(
    primary = Color(0xFFD97757),
    onPrimary = Color(0xFF262624),
    secondary = Color(0xFFB0AEA5),
    background = Color(0xFF262624),
    surface = Color(0xFF30302E),
    surfaceVariant = Color(0xFF454440),
    onBackground = Color(0xFFFAF9F5),
    onSurface = Color(0xFFFAF9F5),
    onSurfaceVariant = Color(0xFFB0AEA5),
)

@Composable
fun WavroApp(viewModel: StudioViewModel = viewModel()) {
    MaterialTheme(colorScheme = AudiovisColors) {
        StudioScreen(viewModel)
    }
}
