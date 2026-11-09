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
fun EqualizerScreen(vm: StudioViewModel) {
    val eqBands by vm.eqBands.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Hardware Equalizer", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Note: This only affects playback, not exports.", color = DM, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        
        if (eqBands.isEmpty()) {
            Text("Play a track to enable the equalizer.", color = DM)
        } else {
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                eqBands.forEachIndexed { index, level ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Hardware EQ usually ranges from -1500 to +1500
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        // Sleep Timer Section
        Text("Sleep Timer", color = TX, fontWeight = FontWeight.Bold)
        if (sleepTimer != null) {
            val m = sleepTimer!! / 60
            val s = sleepTimer!! % 60
            Text("Stopping in: $m:$s", color = AC)
            Button(onClick = { vm.cancelSleepTimer() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Cancel Timer")
            }
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
        
        Text("Video Export", color = TX, fontWeight = FontWeight.Bold)
        Text("Export engine coming in Part 2.", color = DM, fontSize = 12.sp)
    }
}

// [Keep LibraryScreen, VisualizerScreen, VisualizerEditor, PlaceholderScreen, SliderRow, formatTime exactly as they are from the previous message]