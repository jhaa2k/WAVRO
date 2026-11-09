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
import com.wavro.studio.domain.LayerType
import com.wavro.studio.editor.StudioViewModel
import com.wavro.studio.rendering.AudioPreview

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
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    
    val musicLibrary = remember { MusicLibrary(vm.getApplication()) }
    val playbackState by vm.playback.collectAsState()
    val fftData by vm.fftData.collectAsState()
    val project by vm.project.collectAsState()

    LaunchedEffect(Unit) { tracks = musicLibrary.scanDeviceForMusic() }

    Scaffold(
        containerColor = BG,
        topBar = {
            TopAppBar(
                title = { Text("WAVRO STUDIO", color = TX, fontWeight = FontWeight.ExtraBold) },
                actions = {
                    if (currentTab == 2) { // If on Visualizer tab
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
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AC, selectedTextColor = AC, unselectedIconColor = DM, unselectedTextColor = DM)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentTab) {
                0 -> { // Library Tab
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(tracks) { track ->
                            ListItem(
                                headlineContent = { Text(track.title, color = TX) },
                                supportingContent = { Text(track.artist, color = DM) },
                                leadingContent = { Icon(Icons.Default.MusicNote, null, tint = AC) },
                                modifier = Modifier.clickable { 
                                    vm.playTrack(track) 
                                    currentTab = 2 // Automatically switch to visualizer when a song plays
                                }
                            )
                            HorizontalDivider(color = PN)
                        }
                    }
                }
                1 -> { // Playlists Tab Placeholder
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Playlists Feature Coming Next", color = DM)
                    }
                }
                2 -> { // Visualizer Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                            AudioPreview(layers = project.layers, fftData = fftData)
                        }
                        
                        // Playback Controls Bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(playbackState.currentTrack?.title ?: "No Track", color = TX, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.togglePlayback() }) {
                                Icon(
                                    if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = TX,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(formatTime(playbackState.positionMs), color = DM, fontSize = 12.sp)
                        }
                        Slider(
                            value = playbackState.positionMs.toFloat(),
                            onValueChange = { vm.seekTo(it.toLong()) },
                            valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(thumbColor = AC, activeTrackColor = AC)
                        )
                    }
                }
                3 -> { // Equalizer Tab Placeholder
                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Equalizer Coming Next", color = DM)
                    }
                }
                4 -> { // Settings Tab Placeholder
                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Settings Coming Next", color = DM)
                    }
                }
            }

            if (showEditor) {
                ModalBottomSheet(onDismissRequest = { showEditor = false }, containerColor = PN, modifier = Modifier.fillMaxHeight(0.7f)) {
                    LayerEditorPanel(
                        layers = project.layers,
                        onUpdateLayer = { vm.updateLayer(it) },
                        onAddLayer = { vm.addLayer(it) },
                        onDeleteLayer = { vm.deleteLayer(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun LayerEditorPanel(layers: List<com.wavro.studio.domain.Layer>, onUpdateLayer: (com.wavro.studio.domain.Layer) -> Unit, onAddLayer: (LayerType) -> Unit, onDeleteLayer: (String) -> Unit) {
    var selectedLayerId by remember { mutableStateOf<String?>(layers.firstOrNull()?.id) }
    val selectedLayer = layers.find { it.id == selectedLayerId }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Layer Editor", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAddLayer(LayerType.BARS) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("+ Bars") }
            Button(onClick = { onAddLayer(LayerType.CIRCLE) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("+ Circle") }
            Button(onClick = { onAddLayer(LayerType.TEXT) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("+ Text") }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(layers) { layer ->
                Row(modifier = Modifier.fillMaxWidth().clickable { selectedLayerId = layer.id }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (layer.type == LayerType.TEXT) Icons.Default.TextFields else Icons.Default.BarChart, null, tint = if (layer.id == selectedLayerId) AC else TX)
                    Spacer(Modifier.width(8.dp))
                    Text(layer.name, color = if (layer.id == selectedLayerId) AC else TX, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteLayer(layer.id) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            }
        }

        if (selectedLayer != null) {
            Divider(color = DM.copy(alpha = 0.2f))
            Text("Properties: ${selectedLayer.name}", color = TX, fontWeight = FontWeight.Bold)
            SliderProperty("Position X", selectedLayer.x, 0f..100f) { onUpdateLayer(selectedLayer.copy(x = it)) }
            SliderProperty("Position Y", selectedLayer.y, 0f..100f) { onUpdateLayer(selectedLayer.copy(y = it)) }
            
            if (selectedLayer.type == LayerType.TEXT) {
                OutlinedTextField(value = selectedLayer.text.text, onValueChange = { onUpdateLayer(selectedLayer.copy(text = selectedLayer.text.copy(text = it))) }, label = { Text("Text Content") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun SliderProperty(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DM, modifier = Modifier.width(80.dp), fontSize = 12.sp)
        Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = AC, activeTrackColor = AC))
    }
}

fun formatTime(ms: Long): String {
    val sec = ms / 1000
    return "${sec / 60}:${String.format("%02d", sec % 60)}"
}