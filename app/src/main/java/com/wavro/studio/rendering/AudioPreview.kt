package com.wavro.studio.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import com.wavro.studio.audio.PlaybackState
import com.wavro.studio.domain.ElementType
import com.wavro.studio.domain.StudioProject
import kotlin.math.sin

private val Coral = Color(0xFFFF9A6B)
private val Violet = Color(0xFF8F7BFF)

@Composable
fun AudioPreview(
private val Coral = Color(0xFFD97757)
private val Violet = Color(0xFFB0AEA5)
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time -> phase = time / 1_000_000_000f }
        }
    }

    val enabledTypes = remember(project.elements) {
        project.elements.filter { it.enabled }.map { it.type }.toSet()
    }

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxWidth().height(300.dp)) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF17151F), Color(0xFF0D0D12), Color(0xFF15111A)),
                ),
            )
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f + 12.dp.toPx())
            val barsEnabled = enabledTypes.isEmpty() || ElementType.BARS in enabledTypes
                    listOf(Color(0xFF171715), Color.Black, Color(0xFF262624)),
            val particlesEnabled = ElementType.PARTICLES in enabledTypes
            val values = playback.spectrum

            if (radialEnabled) {
                val average = values.average().toFloat().coerceIn(0f, 1f)
                val radius = size.minDimension * 0.19f + average * 15.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Coral.copy(alpha = 0.22f), Violet.copy(alpha = 0.04f), Color.Transparent),
                        center = center,
                        radius = radius * 2.8f,
                    ),
                    radius = radius * 2.8f,
                    center = center,
                )
                drawCircle(
                    color = Coral.copy(alpha = 0.65f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawCircle(
                    color = Violet.copy(alpha = 0.35f),
                    radius = radius + 8.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            if (barsEnabled) {
                val count = 64
                val gap = 3.dp.toPx()
                val barWidth = ((size.width * 0.78f) - gap * (count - 1)) / count
                val baseY = size.height * 0.82f
                val maxHeight = size.height * 0.48f
                repeat(count) { index ->
                    val sample = values.getOrNull(index * values.size / count) ?: 0f
                    val idle = 0.035f + 0.025f * (0.5f + 0.5f * sin(phase * 2.1f + index * 0.17f))
                    val level = if (playback.isPlaying) sample.coerceAtLeast(idle) else idle
                    val barHeight = (maxHeight * (0.06f + level * 0.94f)).coerceAtLeast(3.dp.toPx())
                    val x = (size.width - (count * barWidth + (count - 1) * gap)) / 2f + index * (barWidth + gap)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Violet.copy(alpha = 0.9f), Coral, Coral.copy(alpha = 0.55f)),
                            startY = baseY - barHeight,
                            endY = baseY,
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(x, baseY - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
                    )
                }
            }

            if (particlesEnabled) {
                repeat(34) { index ->
                    val seed = index.toFloat()
                    val x = size.width * (0.08f + ((seed * 0.618f) % 0.84f))
                    val drift = (phase * (0.12f + (index % 4) * 0.025f) + seed * 0.73f) % 1f
                    val y = size.height * (1f - drift)
                    val radius = (1.2f + (index % 3) * 0.55f).dp.toPx()
                    drawCircle(
                        color = if (index % 3 == 0) Violet.copy(alpha = 0.48f) else Coral.copy(alpha = 0.5f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(x, y),
                    )
                }
            }

            if (!playback.isPlaying) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = size.minDimension * 0.31f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }

        Text(
            text = "LIVE PREVIEW",
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
            color = Color.White.copy(alpha = 0.66f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
        )
        Text(
            text = if (playback.isPlaying) "● LIVE" else "READY",
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            color = if (playback.isPlaying) Coral else Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        if (ElementType.TITLE in enabledTypes && playback.title != "No track selected") {
            Text(
                text = playback.title,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 54.dp),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
