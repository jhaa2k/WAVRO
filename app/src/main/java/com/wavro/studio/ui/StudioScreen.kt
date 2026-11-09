package com.wavro.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var showSheet by remember { mutableStateOf(false) }
    
    val playbackState by vm.playback.collectAsState()
    val fftData by vm.fftData.collectAsState()
    val project by vm.project.collectAsState()
    val tracks by vm.libraryTracks.collectAsState()

    Scaffold(
        containerColor = BG,
        topBar = {
            TopAppBar(
                title = { Text("AUDIOVIS PRO", color = TX, fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Undo, null, tint = TX) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Redo, null, tint = TX) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Tune, null, tint = TX) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, null, tint = TX) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BG)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BG) {
                val tabs = listOf(
                    "Media" to Icons.Default.MusicNote,
                    "Layers" to Icons.Default.Layers,
                    "Presets" to Icons.Default.FavoriteBorder,
                    "Audio" to Icons.Default.GraphicEq,
                    "Scene" to Icons.Default.Image,
                    "Export" to Icons.Default.Download
                )
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = currentTab == index && showSheet,
                        onClick = { 
                            currentTab = index
                            showSheet = true 
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AC, selectedTextColor = AC, unselectedIconColor = DM, unselectedTextColor = DM)
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Main Preview Window
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp))) {
                AudioPreview(layers = project.layers, fftData = fftData)
                Text("PREVIEW 1280x720", color = TX, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
            }
            
            // Playback Controls
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(playbackState.currentTrack?.title ?: "No Track", color = TX, fontSize = 12.sp)
                Text(formatTime(playbackState.positionMs) + " / " + formatTime(playbackState.durationMs), color = DM, fontSize = 12.sp)
            }
            Slider(
                value = playbackState.positionMs.toFloat(),
                onValueChange = { vm.seekTo(it.toLong()) },
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = AC)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, null, tint = DM) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, null, tint = TX) }
                Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(50)).background(TX).clickable { vm.togglePlayback() }, contentAlignment = Alignment.Center) {
                    Icon(if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = BG, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, null, tint = TX) }
                IconButton(onClick = {}) { Icon(Icons.Default.Repeat, null, tint = DM) }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = PN) {
            when (currentTab) {
                0 -> MediaTab(tracks) { vm.playTrack(it); showSheet = false }
                1 -> LayersTab(project.layers, vm)
                2 -> PresetsTab()
                3 -> AudioTab(vm)
                4 -> SceneTab()
                5 -> ExportTab(vm)
            }
        }
    }
}

@Composable
fun MediaTab(tracks: List<com.wavro.studio.audio.Track>, onPlay: (com.wavro.studio.audio.Track) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Import", color = DM, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = BG)) {
            Text("Choose audio files", color = TX)
        }
        Text("Queue", color = DM, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        tracks.forEach { track ->
            ListItem(
                headlineContent = { Text(track.title, color = AC) },
                modifier = Modifier.clickable { onPlay(track) }
            )
        }
    }
}

@Composable
fun LayersTab(layers: List<Layer>, vm: StudioViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Layers", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.addLayer(LayerType.BARS) }, colors = ButtonDefaults.buttonColors(containerColor = AC)) { Text("Add layer") }
        }
        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(layers) { layer ->
                ListItem(
                    headlineContent = { Text(layer.name, color = TX) },
                    leadingContent = { Icon(Icons.Default.Layers, null, tint = AC) }
                )
            }
        }
    }
}

@Composable
fun PresetsTab() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Presets", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        val presets = listOf("Classic Vertical", "Circular Pulse", "Line Wave", "Neon Bars")
        presets.forEach { name ->
            ListItem(headlineContent = { Text(name, color = TX) }, trailingContent = { Icon(Icons.Default.FavoriteBorder, null, tint = DM) })
        }
    }
}

@Composable
fun AudioTab(vm: StudioViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Audio", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        val sections = listOf("Playback", "Equalizer", "Effects", "Analysis & Beat", "Modulators (LFO)")
        sections.forEach { name ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = BG)) {
                Text(name, color = TX, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun SceneTab() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Scene", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("-- Custom --", color = DM, modifier = Modifier.padding(vertical = 8.dp))
        val settings = listOf("Background", "Camera zoom", "Vignette", "Scanlines", "Film grain", "Beat flash")
        settings.forEach { name ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text(name, color = DM, modifier = Modifier.width(120.dp), fontSize = 12.sp)
                Slider(value = 0f, onValueChange = {}, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ExportTab(vm: StudioViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Export", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Aspect: 16:9  Quality: 2160p", color = DM, modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = { vm.exportVideo() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AC)) {
            Text("Export Video")
        }
    }
}

fun formatTime(ms: Long): String {
    val sec = ms / 1000
    return "${sec / 60}:${String.format("%02d", sec % 60)}"
}