package com.wavro.studio.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType

@Composable
fun AudioPreview(layers: List<Layer>, fftData: FloatArray, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Color.Black) // Background

        layers.filter { it.visible }.forEach { layer ->
            val cx = size.width * (layer.x / 100f)
            val cy = size.height * (layer.y / 100f)
            
            rotate(layer.rotation, pivot = Offset(cx, cy)) {
                scale(layer.scale / 100f, pivot = Offset(cx, cy)) {
                    when (layer.type) {
                        LayerType.BARS -> drawSpectrumBars(layer, fftData, cx, cy)
                        LayerType.CIRCLE -> drawCircularSpectrum(layer, fftData, cx, cy)
                        LayerType.TEXT -> drawText(layer, cx, cy)
                        else -> Unit
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSpectrumBars(layer: Layer, fft: FloatArray, cx: Float, cy: Float) {
    val barCount = layer.spectrum.barCount
    val totalWidth = size.width * 0.8f
    val spacing = totalWidth / barCount
    val barWidth = spacing * 0.7f
    val maxHeight = size.height * 0.3f
    val startX = cx - (totalWidth / 2f)

    for (i in 0 until barCount) {
        val fftIndex = (i.toFloat() / barCount * fft.size).toInt().coerceIn(0, fft.size - 1)
        val amplitude = (fft[fftIndex] * layer.spectrum.sensitivity).coerceIn(0.01f, 1f)
        val barHeight = (maxHeight * amplitude).coerceAtLeast(4f)

        drawRect(
            color = Color(layer.colorPrimary),
            topLeft = Offset(startX + (i * spacing), cy - barHeight),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawCircularSpectrum(layer: Layer, fft: FloatArray, cx: Float, cy: Float) {
    val barCount = layer.spectrum.barCount
    val radius = size.minDimension * 0.15f
    val maxBarLength = size.minDimension * 0.2f

    for (i in 0 until barCount) {
        val fftIndex = (i.toFloat() / barCount * fft.size).toInt().coerceIn(0, fft.size - 1)
        val amplitude = (fft[fftIndex] * layer.spectrum.sensitivity).coerceIn(0.01f, 1f)
        val barLength = (maxBarLength * amplitude).coerceAtLeast(4f)
        val angle = (i.toFloat() / barCount) * (Math.PI * 2).toFloat() - (Math.PI / 2).toFloat()
        
        val startX = cx + kotlin.math.cos(angle) * radius
        val startY = cy + kotlin.math.sin(angle) * radius
        val endX = cx + kotlin.math.cos(angle) * (radius + barLength)
        val endY = cy + kotlin.math.sin(angle) * (radius + barLength)

        drawLine(color = Color(layer.colorPrimary), start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 6f)
    }
}

private fun DrawScope.drawText(layer: Layer, cx: Float, cy: Float) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = layer.colorPrimary
            textSize = layer.text.textSize * density * 2f // Scaled up for visibility
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (layer.text.isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        drawText(layer.text.text, cx, cy, paint)
    }
}