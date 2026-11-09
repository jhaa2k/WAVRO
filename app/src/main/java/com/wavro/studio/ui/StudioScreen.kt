package com.wavro.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private val BG = Color(0xFF262624)
private val PN = Color(0xFF30302E)
private val LN = Color(0xFF454440)
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
                title = { Text("AUDIOVIS PRO", color = TX, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) },
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
                        onClick = { currentTab = index; showSheet = true },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                        label = { Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AC, selectedTextColor = AC,
                            unselectedIconColor = DM, unselectedTextColor = DM,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Main Canvas / Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, LN, RoundedCornerShape(12.dp))
            ) {
                AudioPreview(layers = project.layers, fftData = fftData)
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PREVIEW 1280x720", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Controls
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(playbackState.currentTrack?.title ?: "No Track", color = DM, fontSize = 12.sp)
                Text("${formatTime(playbackState.positionMs)} / ${formatTime(playbackState.durationMs)}", color = DM, fontSize = 12.sp)
            }
            Slider(
                value = playbackState.positionMs.toFloat(),
                onValueChange = { vm.seekTo(it.toLong()) },
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = AC),
                modifier = Modifier.padding(horizontal = 16.dp)
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
        ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = PN, dragHandle = { BottomSheetDefaults.DragHandle(color = LN) }) {
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

// ==================== LAYERS TAB WITH EDITOR ====================
@Composable
fun LayersTab(layers: List<Layer>, vm: StudioViewModel) {
    val selectedId by vm.selectedLayerId.collectAsState()
    val selectedLayer = layers.find { it.id == selectedId }

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth().height(600.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Layers", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Default.Close, null, tint = TX) }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Box(modifier = Modifier.weight(1f).background(BG, RoundedCornerShape(10.dp)).padding(12.dp)) {
                Text("bars", color = DM)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.addLayer(LayerType.BARS) }, colors = ButtonDefaults.buttonColors(containerColor = AC), shape = RoundedCornerShape(20.dp)) {
                Text("Add layer", color = Color.White)
            }
        }
        
        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
            items(layers) { layer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BG)
                        .border(1.dp, if (layer.id == selectedId) AC else LN, RoundedCornerShape(10.dp))
                        .clickable { vm.selectLayer(layer.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(layer.name, color = if (layer.id == selectedId) AC else TX, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Visibility, null, tint = DM)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.LockOpen, null, tint = DM)
                }
            }
        }

        // --- THE MISSING LAYER EDITOR (From your screenshot) ---
        if (selectedLayer != null) {
            HorizontalDivider(color = LN, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text("Transform", color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    SliderRow("X Position", selectedLayer.x, 0f..100f) { vm.updateLayer(selectedLayer.copy(x = it)) }
                    SliderRow("Y Position", selectedLayer.y, 0f..100f) { vm.updateLayer(selectedLayer.copy(y = it)) }
                    SliderRow("Scale", selectedLayer.scale, 10f..300f) { vm.updateLayer(selectedLayer.copy(scale = it)) }
                    
                    Text("Rotate", color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    SliderRow("Rotation", selectedLayer.rotation, 0f..360f) { vm.updateLayer(selectedLayer.copy(rotation = it)) }
                }
            }
        }
    }
}

@Composable
fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = DM, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = TX, activeTrackColor = LN))
        Text("${value.toInt()}", color = DM, fontSize = 12.sp, modifier = Modifier.width(30.dp))
    }
}

// Baki Tabs (Media, Presets, Audio, Scene, Export) same rahenge...
// (Copy from previous code)
@Composable
fun MediaTab(tracks: List<com.wavro.studio.audio.Track>, onPlay: (com.wavro.studio.audio.Track) -> Unit) { /* ... */ }
@Composable
fun PresetsTab() { /* ... */ }
@Composable
fun AudioTab(vm: StudioViewModel) { /* ... */ }
@Composable
fun SceneTab() { /* ... */ }
@Composable
fun ExportTab(vm: StudioViewModel) { /* ... */ }
fun formatTime(ms: Long): String { val sec = ms / 1000; return "${sec / 60}:${String.format("%02d", sec % 60)}" }