package com.wavro.studio.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType
import com.wavro.studio.visualization.SpectrumMapper
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioPreview(
    layers: List<Layer>,
    bands: SpectrumMapper.Bands,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw dark canvas background
        drawRect(Color.Black)

        layers.filter { it.visible }.forEach { layer ->
            when (layer.type) {
                LayerType.BARS -> drawBars(layer, bands)
                LayerType.CIRCLE -> drawCircleBars(layer, bands)
                LayerType.WAVE -> drawWave(layer, bands)
                LayerType.DOTS -> drawDots(layer, bands)
                LayerType.PARTICLES -> drawParticles(layer, bands)
                LayerType.TEXT -> drawTextLayer(layer)
                LayerType.CONCENTRIC -> drawConcentric(layer, bands)
                LayerType.BACKGROUND -> drawRect(
                    color = Color(layer.colorPrimary),
                    size = size
                )
                else -> Unit
            }
        }
    }
}

private fun DrawScope.drawBars(layer: Layer, b: SpectrumMapper.Bands) {
    val span = size.width * 0.8f
    val len = size.height * 0.35f * (layer.barLength / 100f)
    val n = layer.barCount
    val sl = span / n
    val bw = sl * (layer.barThickness / 100f)
    val x0 = (size.width - span) / 2f
    val y0 = size.height * (layer.y / 100f)
    val bands = floatArrayOf(b.bass, b.lowMid, b.mid, b.upMid, b.presence, b.brill)
    repeat(n) { i ->
        val v = bands[i % bands.size]
        val h = (len * v).coerceAtLeast(1f)
        drawRect(
            color = Color(layer.colorPrimary),
            topLeft = Offset(x0 + i * sl + (sl - bw) / 2f, y0 - h),
            size = Size(bw, h)
        )
    }
}

private fun DrawScope.drawCircleBars(layer: Layer, b: SpectrumMapper.Bands) {
    val cx = size.width * (layer.x / 100f)
    val cy = size.height * (layer.y / 100f)
    val r = minOf(size.width, size.height) * 0.22f
    val n = layer.barCount
    val bands = floatArrayOf(b.bass, b.lowMid, b.mid, b.upMid, b.presence, b.brill)
    repeat(n) { i ->
        val a = (i.toFloat() / n) * (Math.PI * 2).toFloat() - (Math.PI / 2).toFloat()
        val v = bands[i % bands.size]
        val inner = r
        val outer = r + r * 0.5f * v * (layer.barLength / 100f)
        drawLine(
            color = Color(layer.colorPrimary),
            start = Offset(cx + cos(a) * inner, cy + sin(a) * inner),
            end = Offset(cx + cos(a) * outer, cy + sin(a) * outer),
            strokeWidth = 4f
        )
    }
}

private fun DrawScope.drawWave(layer: Layer, b: SpectrumMapper.Bands) {
    val span = size.width * 0.8f
    val amp = size.height * 0.2f * (layer.barLength / 100f)
    val cx = size.width / 2f
    val cy = size.height * (layer.y / 100f)
    val points = 100
    val path = androidx.compose.ui.graphics.Path()
    repeat(points) { i ->
        val x = cx - span / 2f + span * i / points
        val y = cy + sin(i * 0.3f + b.volume * 10f) * amp * b.volume
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, Color(layer.colorPrimary), style = Stroke(width = 3f))
}

private fun DrawScope.drawDots(layer: Layer, b: SpectrumMapper.Bands) {
    val span = size.width * 0.8f
    val n = layer.barCount
    val sl = span / n
    val x0 = (size.width - span) / 2f
    val y0 = size.height * (layer.y / 100f)
    repeat(n) { i ->
        val v = (b.bass + b.mid) / 2f
        drawCircle(
            color = Color(layer.colorPrimary),
            radius = 4f + v * 10f,
            center = Offset(x0 + i * sl + sl / 2f, y0)
        )
    }
}

private fun DrawScope.drawParticles(layer: Layer, b: SpectrumMapper.Bands) {
    // Minimal deterministic particle representation for now.
    val n = 30
    val cx = size.width / 2f
    val cy = size.height / 2f
    repeat(n) { i ->
        val a = i * 0.7f
        val r = 80f + b.bass * 200f * (i % 3)
        drawCircle(
            color = Color(layer.colorPrimary).copy(alpha = 0.7f),
            radius = 3f,
            center = Offset(cx + cos(a) * r, cy + sin(a) * r)
        )
    }
}

private fun DrawScope.drawTextLayer(layer: Layer) {
    // Placeholder: uses Compose's native Text via overlay in StudioScreen.
    drawRect(
        color = Color(layer.colorPrimary).copy(alpha = 0.2f),
        topLeft = Offset(size.width * 0.3f, size.height * 0.4f),
        size = Size(size.width * 0.4f, size.height * 0.1f)
    )
}

private fun DrawScope.drawConcentric(layer: Layer, b: SpectrumMapper.Bands) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    repeat(5) { i ->
        val r = 40f + i * 30f + b.bass * 50f
        drawCircle(
            color = Color(layer.colorPrimary).copy(alpha = 0.5f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )
    }
}