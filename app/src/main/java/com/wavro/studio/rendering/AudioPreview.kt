package com.wavro.studio.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType

@Composable
fun AudioPreview(layers: List<Layer>, fftData: FloatArray, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // 1. Draw Dark Background
        drawRect(Color.Black)

        // 2. Draw Center Crosshair Grid (Like your screenshot)
        drawLine(Color.Gray.copy(alpha = 0.3f), start = Offset(size.width / 2, 0f), end = Offset(size.width / 2, size.height), strokeWidth = 1f)
        drawLine(Color.Gray.copy(alpha = 0.3f), start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 1f)

        // 3. Draw Layers
        layers.filter { it.visible }.forEach { layer ->
            val cx = size.width * (layer.x / 100f)
            val cy = size.height * (layer.y / 100f)
            
            when (layer.type) {
                LayerType.BARS -> {
                    // THIS DRAWS THE SPECTRUM BARS FROM YOUR SCREENSHOT
                    val barCount = layer.spectrum.barCount
                    val totalWidth = size.width * 0.8f
                    val spacing = totalWidth / barCount
                    val barWidth = spacing * 0.7f
                    val maxHeight = size.height * 0.3f
                    val startX = cx - (totalWidth / 2f)

                    for (i in 0 until barCount) {
                        val fftIndex = (i.toFloat() / barCount * fftData.size).toInt().coerceIn(0, fftData.size - 1)
                        val amplitude = (fftData[fftIndex] * layer.spectrum.sensitivity).coerceIn(0.01f, 1f)
                        val barHeight = (maxHeight * amplitude).coerceAtLeast(4f)

                        drawRect(
                            // Cyan color from your screenshot
                            color = Color(0xFF00BFFF), 
                            topLeft = Offset(startX + (i * spacing), cy - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
                LayerType.TEXT -> {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = layer.colorPrimary
                            textSize = 50f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(layer.text.text, cx, cy, paint)
                    }
                }
                else -> Unit
            }
        }
    }
}