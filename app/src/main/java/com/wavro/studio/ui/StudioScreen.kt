package com.wavro.studio.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavro.studio.domain.LayerType
import com.wavro.studio.editor.StudioViewModel
import com.wavro.studio.rendering.AudioPreview
import kotlin.math.roundToInt

// Exact Colors from your CSS
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
            NavigationBar(containerColor = BG, modifier = Modifier.border(1.dp, LN, RoundedCornerShape(0.dp))) {
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
            // Preview Box
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
                
                // HUD
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PREVIEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp, 2.dp))
                    Text("1280x720", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp, 2.dp))
                }
            }
            
            // Track Info
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(playbackState.currentTrack?.title ?: "No track loaded", color = DM, fontSize = 12.sp, maxLines = 1)
                Text("${formatTime(playbackState.positionMs)} / ${formatTime(playbackState.durationMs)}", color = DM, fontSize = 12.sp)
            }
            
            // Seek Bar
            Slider(
                value = playbackState.positionMs.toFloat(),
                onValueChange = { vm.seekTo(it.toLong()) },
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = AC, inactiveTrackColor = LN),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
            )
            
            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, null, tint = DM) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, null, tint = TX) }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(TX)
                        .clickable { vm.togglePlayback() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = BG, modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, null, tint = TX) }
                IconButton(onClick = {}) { Icon(Icons.Default.Repeat, null, tint = DM) }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = PN,
            dragHandle = { BottomSheetDefaults.DragHandle(color = LN) }
        ) {
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

// ==================== TABS ====================

@Composable
fun MediaTab(tracks: List<com.wavro.studio.audio.Track>, onPlay: (com.wavro.studio.audio.Track) -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text("Import", color = DM, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BG),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Choose audio files", color = TX, fontSize = 14.sp) }
        
        Text("Queue", color = DM, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(tracks) { track ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (track.title == tracks.firstOrNull()?.title) AC.copy(alpha = 0.2f) else BG)
                        .border(1.dp, if (track.title == tracks.firstOrNull()?.title) AC else LN, RoundedCornerShape(10.dp))
                        .clickable { onPlay(track) }
                ) {
                    Text(
                        text = track.title,
                        color = if (track.title == tracks.firstOrNull()?.title) AC else TX,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
        
        Text("Sleep timer", color = DM, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = BG), modifier = Modifier.padding(top = 4.dp)) {
            Text("Off", color = TX)
            Icon(Icons.Default.ArrowDropDown, null, tint = DM)
        }
        Text("Audio is decoded in memory. Files stay on this device.", color = DM, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun LayersTab(layers: List<com.wavro.studio.domain.Layer>, vm: StudioViewModel) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
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
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(layers) { layer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BG)
                        .border(1.dp, if (layer.id == vm.selectedLayerId.value) AC else LN, RoundedCornerShape(10.dp))
                        .clickable { vm.selectLayer(layer.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(layer.name, color = if (layer.id == vm.selectedLayerId.value) AC else TX, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Visibility, null, tint = DM)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.LockOpen, null, tint = DM)
                }
            }
        }
    }
}

@Composable
fun PresetsTab() {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Presets", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Default.Close, null, tint = TX) }
        }
        TextField(
            value = "", onValueChange = {},
            placeholder = { Text("Search presets", color = DM) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BG, unfocusedContainerColor = BG,
                focusedTextColor = TX, unfocusedTextColor = TX
            ),
            shape = RoundedCornerShape(10.dp)
        )
        val presets = listOf(
            "Classic Vertical" to "Minimal",
            "Circular Pulse" to "Circular",
            "Line Wave" to "Waveform",
            "Neon Bars" to "Neon",
            "Particle Burst" to "Particles",
            "Beat Title" to "Typography"
        )
        presets.forEach { (name, cat) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BG)
                    .border(1.dp, LN, RoundedCornerShape(10.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = TX, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Text(cat, color = DM, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.FavoriteBorder, null, tint = DM)
            }
        }
    }
}

@Composable
fun AudioTab(vm: StudioViewModel) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Audio", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Default.Close, null, tint = TX) }
        }
        val sections = listOf("Playback", "Equalizer", "Effects", "Analysis & Beat", "Modulators (LFO)")
        sections.forEach { name ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BG)
                    .border(1.dp, LN, RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {
                Text(name, color = TX, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SceneTab() {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scene", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Default.Close, null, tint = TX) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Text("Scene Preset", color = DM, fontSize = 12.sp, modifier = Modifier.width(100.dp))
            Box(modifier = Modifier.weight(1f).background(BG, RoundedCornerShape(10.dp)).border(1.dp, LN, RoundedCornerShape(10.dp)).padding(12.dp)) {
                Text("-- Custom --", color = DM)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BG)
                .border(1.dp, LN, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                val settings = listOf("Background", "Camera zoom", "Vignette", "Scanlines", "Film grain", "Beat flash", "Beat shake", "Hue jump per beat")
                settings.forEach { name ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(name, color = DM, fontSize = 12.sp, modifier = Modifier.width(120.dp))
                        Slider(value = 0f, onValueChange = {}, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = TX, activeTrackColor = LN))
                        Text("0", color = DM, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExportTab(vm: StudioViewModel) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Export", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Default.Close, null, tint = TX) }
        }
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Aspect", color = DM, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth().background(BG, RoundedCornerShape(10.dp)).border(1.dp, LN, RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Text("16:9", color = TX)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Quality", color = DM, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth().background(BG, RoundedCornerShape(10.dp)).border(1.dp, LN, RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Text("2160p", color = TX)
                }
            }
        }
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("FPS", color = DM, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth().background(BG, RoundedCornerShape(10.dp)).border(1.dp, LN, RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Text("60", color = TX)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Format", color = DM, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth().background(BG, RoundedCornerShape(10.dp)).border(1.dp, LN, RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Text("MP4 (H.264)", color = TX)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BG)
                .border(1.dp, LN, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Video Mbps", color = DM, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                    Slider(value = 8f, onValueChange = {}, valueRange = 0f..40f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = TX, activeTrackColor = LN))
                    Text("8", color = DM, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Audio kbps", color = DM, fontSize = 12.sp, modifier = Modifier.width(100.dp))
                    Slider(value = 192f, onValueChange = {}, valueRange = 0f..320f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = TX, activeTrackColor = LN))
                    Text("192", color = DM, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                }
            }
        }
        Button(
            onClick = { vm.exportVideo() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AC),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Export Video", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatTime(ms: Long): String {
    val sec = ms / 1000
    return "${sec / 60}:${String.format("%02d", sec % 60)}"
}