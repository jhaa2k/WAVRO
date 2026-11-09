package com.wavro.studio.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType
import kotlin.math.sin

@Composable
fun AudioPreview(
    layers: List<Layer>,
    audioAmplitude: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw Background
        drawRect(Color.Black)

        layers.filter { it.visible }.forEach { layer ->
            when (layer.type) {
                LayerType.BARS -> {
                    val barCount = layer.spectrum.barCount
                    val spacing = size.width / barCount
                    val barWidth = spacing * 0.7f
                    val baseColor = Color(layer.colorPrimary)

                    // Fake spectrum data based on amplitude for demonstration
                    // In a real app, you would pass the actual FFT data here
                    for (i in 0 until barCount) {
                        val fakeFrequency = (sin(i.toDouble() * 0.5) * audioAmplitude * 500).toFloat()
                        val height = (size.height * 0.4f) * (fakeFrequency / 500f).coerceIn(0.05f, 1f)
                        
                        drawRect(
                            color = baseColor,
                            topLeft = Offset(i * spacing, size.height - height),
                            size = Size(barWidth, height)
                        )
                    }
                }
                LayerType.CIRCLE -> {
                    // Simple reactive circle
                    drawCircle(
                        color = Color(layer.colorPrimary).copy(alpha = 0.5f),
                        radius = (size.minDimension * 0.2f) + (audioAmplitude * size.minDimension * 0.1f),
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
                else -> Unit
            }
        }
    }
}