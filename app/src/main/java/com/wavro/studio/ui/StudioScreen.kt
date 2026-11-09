package com.wavro.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavro.studio.audio.MusicLibrary
import com.wavro.studio.audio.Track
import com.wavro.studio.editor.StudioViewModel
import com.wavro.studio.rendering.AudioPreview

private val BG = Color(0xFF262624)
private val PN = Color(0xFF30302E)
private val TX = Color(0xFFFAF9F5)
private val DM = Color(0xFFB0AEA5)
private val AC = Color(0xFFD97757)

@Composable
fun StudioScreen(vm: StudioViewModel) {
    var currentScreen by remember { mutableStateOf("Library") }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    val musicLibrary = remember { MusicLibrary(vm.getApplication()) }

    LaunchedEffect(Unit) {
        tracks = musicLibrary.scanDeviceForMusic()
    }

    val playbackState by vm.playback.collectAsState()

    Scaffold(
        containerColor = BG,
        bottomBar = {
            NavigationBar(containerColor = PN) {
                NavigationBarItem(
                    selected = currentScreen == "Library",
                    onClick = { currentScreen = "Library" },
                    icon = { Icon(Icons.Default.LibraryMusic, null) },
                    label = { Text("Library") }
                )
                NavigationBarItem(
                    selected = currentScreen == "Visualizer",
                    onClick = { currentScreen = "Visualizer" },
                    icon = { Icon(Icons.Default.GraphicEq, null) },
                    label = { Text("Visualizer") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentScreen) {
                "Library" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(tracks) { track ->
                            ListItem(
                                headlineContent = { Text(track.title, color = TX) },
                                supportingContent = { Text(track.artist, color = DM) },
                                modifier = Modifier.clickable { vm.playTrack(track) }
                            )
                            Divider(color = PN)
                        }
                    }
                }
                "Visualizer" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Visualizer Canvas
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                            AudioPreview(
                                layers = vm.project.value.layers,
                                audioAmplitude = if (playbackState.isPlaying) 1f else 0f
                            )
                        }
                        
                        // Playback Controls
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { vm.togglePlayback() }) {
                                Icon(
                                    if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = TX
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}