package com.audiovis.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppBg = Color(0xFF262624)
private val Panel = Color(0xFF30302E)
private val Line = Color(0xFF454440)
private val TextMain = Color(0xFFFAF9F5)
private val TextDim = Color(0xFFB0AEA5)
private val Accent = Color(0xFFD97757)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AudiovisApp() }
    }
}

private enum class StudioTab(val title: String) {
    Media("Media"), Layers("Layers"), Presets("Presets"),
    Audio("Audio"), Scene("Scene"), Export("Export")
}

@Composable
fun AudiovisApp() {
    var selectedTab by remember { mutableStateOf(StudioTab.Media) }
    var playing by remember { mutableStateOf(false) }
    var trackName by remember { mutableStateOf("No track loaded") }
    var showSettings by remember { mutableStateOf(false) }
    var showPanel by remember { mutableStateOf(true) }

    MaterialTheme(colorScheme = darkColorScheme(
        background = AppBg, surface = Panel, primary = Accent,
        onBackground = TextMain, onSurface = TextMain
    )) {
        Column(
            Modifier.fillMaxSize().background(AppBg)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AUDIOVIS PRO", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Box(Modifier.padding(start = 10.dp).size(5.dp).clip(CircleShape).background(if (playing) Accent else Line))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, "Settings", tint = TextMain)
                }
            }

            Column(Modifier.weight(1f).fillMaxWidth()) {
                // The preview uses a native drawing surface; the OpenGL renderer is added in the rendering milestone.
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val centerY = size.height / 2f
                        drawLine(Color(0xFF292929), androidx.compose.ui.geometry.Offset(0f, centerY),
                            androidx.compose.ui.geometry.Offset(size.width, centerY), 1.dp.toPx())
                        if (playing) {
                            val bars = 48
                            for (i in 0 until bars) {
                                val x = size.width * (i + 0.5f) / bars
                                val h = (12 + ((i * 37) % 55)).dp.toPx()
                                drawLine(Accent.copy(alpha = .8f),
                                    androidx.compose.ui.geometry.Offset(x, centerY - h),
                                    androidx.compose.ui.geometry.Offset(x, centerY + h),
                                    2.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                    }
                    Column(Modifier.align(Alignment.TopStart).padding(10.dp)) {
                        Text("PREVIEW", color = TextMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("960 × 540", color = TextDim, fontSize = 9.sp)
                    }
                }

                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(trackName, color = TextDim, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("0:00 / 0:00", color = TextDim, fontSize = 11.sp)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerButton(Icons.Default.Shuffle, "Shuffle") {}
                        PlayerButton(Icons.Default.SkipPrevious, "Previous") {}
                        FilledIconButton(
                            onClick = { playing = !playing },
                            modifier = Modifier.size(54.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = TextMain, contentColor = AppBg
                            )
                        ) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or pause", modifier = Modifier.size(26.dp))
                        }
                        PlayerButton(Icons.Default.SkipNext, "Next") {}
                        PlayerButton(Icons.Default.Repeat, "Repeat") {}
                    }
                    Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp)).background(Panel)) {
                        Canvas(Modifier.fillMaxSize()) {
                            for (i in 0 until 100) {
                                val x = size.width * i / 100f
                                val h = ((i * 19) % 25 + 3).toFloat()
                                drawLine(Accent.copy(alpha = .7f),
                                    androidx.compose.ui.geometry.Offset(x, size.height / 2 - h / 2),
                                    androidx.compose.ui.geometry.Offset(x, size.height / 2 + h / 2),
                                    1.dp.toPx())
                            }
                        }
                    }
                }

                if (showPanel) {
                    Column(
                        Modifier.fillMaxWidth().weight(.82f).background(Panel)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedTab.title, color = TextMain, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showPanel = false }) {
                                Icon(Icons.Default.Close, "Close panel", tint = TextDim)
                            }
                        }
                        when (selectedTab) {
                            StudioTab.Media -> {
                                Text("Import audio and media to begin.", color = TextDim, fontSize = 12.sp)
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(onClick = { trackName = "Media picker integration pending" }) {
                                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add media")
                                }
                            }
                            StudioTab.Layers -> PanelMessage("Layer stack and layer properties")
                            StudioTab.Presets -> PanelMessage("Visualizer presets")
                            StudioTab.Audio -> PanelMessage("Spectrum, FFT and beat controls")
                            StudioTab.Scene -> PanelMessage("Canvas size, background and scene settings")
                            StudioTab.Export -> PanelMessage("Export resolution, frame rate and format")
                        }
                    }
                } else {
                    TextButton(onClick = { showPanel = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Show ${selectedTab.title} panel", color = Accent)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().height(58.dp).border(.5.dp, Line)
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StudioTab.entries.forEach { tab ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable {
                            selectedTab = tab
                            showPanel = true
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val icon = when (tab) {
                            StudioTab.Media -> Icons.Default.MusicNote
                            StudioTab.Layers -> Icons.Default.Layers
                            StudioTab.Presets -> Icons.Default.FavoriteBorder
                            StudioTab.Audio -> Icons.Default.Equalizer
                            StudioTab.Scene -> Icons.Default.Image
                            StudioTab.Export -> Icons.Default.FileDownload
                        }
                        Icon(icon, null, tint = if (selectedTab == tab) Accent else TextDim,
                            modifier = Modifier.size(21.dp))
                        Text(tab.title, color = if (selectedTab == tab) TextMain else TextDim,
                            fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Settings") },
                text = {
                    Column {
                        Text("AUDIOVIS Studio Pro", color = TextMain)
                        Text("Native Android foundation", color = TextDim, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Preview quality, frame limit, themes and performance settings will be connected in the next implementation milestone.",
                            color = TextDim, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) { Text("Close") }
                },
                containerColor = Panel,
                titleContentColor = TextMain
            )
        }
    }
}

@Composable
private fun PlayerButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
        Icon(icon, contentDescription = label, tint = TextMain, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PanelMessage(message: String) {
    Text(message, color = TextDim, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    Text("Feature module scaffold — implementation pending.", color = TextDim, fontSize = 11.sp)
}
