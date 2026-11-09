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
import com.wavro.studio.domain.Layer
import com.wavro.studio.domain.LayerType
import com.wavro.studio.editor.StudioViewModel
import com.wavro.studio.rendering.AudioPreview
import kotlin.math.roundToInt

private val BG = Color(0xFF262624)
private val PN = Color(0xFF30302E)
private val TX = Color(0xFFFAF9F5)
private val DM = Color(0xFFB0AEA5)
private val AC = Color(0xFFD97757)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(vm: StudioViewModel) {
    var currentTab by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    
    val playbackState by vm.playback.collectAsState()
    val fftData by vm.fftData.collectAsState()
    val project by vm.project.collectAsState()
    val tracks by vm.libraryTracks.collectAsState()

    Scaffold(
        containerColor = BG,
        topBar = {
            TopAppBar(
                title = { Text("WAVRO STUDIO", color = TX, fontWeight = FontWeight.ExtraBold) },
                actions = {
                    if (currentTab == 2) {
                        IconButton(onClick = { showEditor = !showEditor }) {
                            Icon(Icons.Default.Edit, "Editor", tint = AC)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BG)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = PN) {
                val tabs = listOf(
                    "Library" to Icons.Default.LibraryMusic,
                    "Playlists" to Icons.Default.QueueMusic,
                    "Visualizer" to Icons.Default.GraphicEq,
                    "Equalizer" to Icons.Default.Equalizer,
                    "Settings" to Icons.Default.Settings
                )
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AC, selectedTextColor = AC, unselectedIconColor = DM, unselectedTextColor = DM)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentTab) {
                0 -> LibraryScreen(tracks) { vm.playTrack(it); currentTab = 2 }
                1 -> PlaceholderScreen("Playlists (Queue, Favorites, Folders)")
                2 -> VisualizerScreen(project.layers, fftData, playbackState, vm)
                3 -> EqualizerScreen(vm)
                4 -> SettingsScreen(vm)
            }

            if (showEditor) {
                ModalBottomSheet(onDismissRequest = { showEditor = false }, containerColor = PN, modifier = Modifier.fillMaxHeight(0.8f)) {
                    VisualizerEditor(project.layers, vm)
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(tracks: List<com.wavro.studio.audio.Track>, onPlay: (com.wavro.studio.audio.Track) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTracks = tracks.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs...", color = DM) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TX, unfocusedTextColor = TX, focusedBorderColor = AC, unfocusedBorderColor = DM)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredTracks) { track ->
                ListItem(
                    headlineContent = { Text(track.title, color = TX) },
                    supportingContent = { Text(track.artist, color = DM) },
                    leadingContent = { Icon(Icons.Default.MusicNote, null, tint = AC) },
                    modifier = Modifier.clickable { onPlay(track) }
                )
                HorizontalDivider(color = PN)
            }
        }
    }
}

@Composable
fun VisualizerScreen(layers: List<Layer>, fft: FloatArray, playbackState: com.wavro.studio.audio.PlaybackState, vm: StudioViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            AudioPreview(layers = layers, fftData = fft)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(playbackState.currentTrack?.title ?: "No Track", color = TX, fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.togglePlayback() }) {
                Icon(if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = TX, modifier = Modifier.size(36.dp))
            }
            Text(formatTime(playbackState.positionMs), color = DM, fontSize = 12.sp)
        }
        Slider(
            value = playbackState.positionMs.toFloat(),
            onValueChange = { vm.seekTo(it.toLong()) },
            valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(thumbColor = AC, activeTrackColor = AC),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun VisualizerEditor(layers: List<Layer>, vm: StudioViewModel) {
    val selectedId by vm.selectedLayerId.collectAsState()
    val selectedLayer = layers.find { it.id == selectedId }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Editor", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Button(onClick = { vm.addLayer(LayerType.BARS) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("+ Bars") }
            Button(onClick = { vm.addLayer(LayerType.CIRCLE) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("+ Circle") }
            Button(onClick = { vm.addLayer(LayerType.TEXT) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("+ Text") }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(layers) { layer ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { vm.selectLayer(layer.id) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (layer.type == LayerType.TEXT) Icons.Default.TextFields else Icons.Default.BarChart, null, tint = if (layer.id == selectedId) AC else TX)
                    Spacer(Modifier.width(8.dp))
                    Text(layer.name, color = if (layer.id == selectedId) AC else TX, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteLayer(layer.id) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            }
        }

        if (selectedLayer != null) {
            Divider(color = DM.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
            Text("Editing: ${selectedLayer.name}", color = TX, fontWeight = FontWeight.Bold)
            SliderRow("X Position", selectedLayer.x, 0f..100f) { vm.updateLayer(selectedLayer.copy(x = it)) }
            SliderRow("Y Position", selectedLayer.y, 0f..100f) { vm.updateLayer(selectedLayer.copy(y = it)) }
            SliderRow("Scale", selectedLayer.scale, 10f..300f) { vm.updateLayer(selectedLayer.copy(scale = it)) }
            SliderRow("Rotation", selectedLayer.rotation, 0f..360f) { vm.updateLayer(selectedLayer.copy(rotation = it)) }
            
            if (selectedLayer.type == LayerType.TEXT) {
                OutlinedTextField(
                    value = selectedLayer.text.text,
                    onValueChange = { vm.updateLayer(selectedLayer.copy(text = selectedLayer.text.copy(text = it))) },
                    label = { Text("Text Content") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun EqualizerScreen(vm: StudioViewModel) {
    val eqBands by vm.eqBands.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Hardware Equalizer", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        if (eqBands.isEmpty()) {
            Text("Play a track to enable the equalizer.", color = DM)
        } else {
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                eqBands.forEachIndexed { index, level ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { vm.setEqBand(index.toShort(), it.roundToInt().toShort()) },
                            valueRange = -1500f..1500f,
                            modifier = Modifier.height(250.dp).width(40.dp),
                            colors = SliderDefaults.colors(thumbColor = AC, activeTrackColor = AC)
                        )
                        Text("Band $index", color = DM, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: StudioViewModel) {
    val sleepTimer by vm.sleepTimerRemaining.collectAsState()
    val isExporting by vm.isExporting.collectAsState()
    val exportProgress by vm.exportProgress.collectAsState()
    val exportStatus by vm.exportStatus.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        Text("Sleep Timer", color = TX, fontWeight = FontWeight.Bold)
        if (sleepTimer != null) {
            val m = sleepTimer!! / 60
            val s = sleepTimer!! % 60
            Text("Stopping in: $m:$s", color = AC)
            Button(onClick = { vm.cancelSleepTimer() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Cancel Timer") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.startSleepTimer(15) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("15 Min") }
                Button(onClick = { vm.startSleepTimer(30) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("30 Min") }
                Button(onClick = { vm.startSleepTimer(60) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("60 Min") }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Divider(color = PN)
        Spacer(Modifier.height(16.dp))
        
        Text("Video Export (MP4)", color = TX, fontWeight = FontWeight.Bold)
        Text("Renders the visualizer at 1080x1920 (30fps).", color = DM, fontSize = 12.sp)
        
        Spacer(Modifier.height(8.dp))
        if (isExporting) {
            LinearProgressIndicator(progress = { exportProgress }, modifier = Modifier.fillMaxWidth(), color = AC)
            Text(exportStatus, color = AC, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            Button(
                onClick = { vm.exportVideo() },
                colors = ButtonDefaults.buttonColors(containerColor = AC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Start Export")
            }
            if (exportStatus.isNotEmpty()) {
                Text(exportStatus, color = DM, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, color = DM, fontSize = 16.sp)
    }
}

@Composable
fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = DM, modifier = Modifier.width(80.dp), fontSize = 12.sp)
        Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = AC, activeTrackColor = AC))
    }
}

fun formatTime(ms: Long): String {
    val sec = ms / 1000
    return "${sec / 60}:${String.format("%02d", sec % 60)}"
}