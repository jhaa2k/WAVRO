package com.example.audiovispro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudiovisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AudiovisApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiovisApp() {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopHeader() },
        bottomBar = { 
            BottomNav(
                selectedTab = selectedTab,
                onTabSelected = { 
                    selectedTab = it
                    showBottomSheet = true 
                }
            ) 
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Stage / Preview Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                ) {
                    // HUD
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PREVIEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp, 2.dp))
                        Text("60 FPS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp, 2.dp))
                    }
                }

                // Track Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("No track loaded", color = Dm, fontSize = 11.sp)
                    Text("0:00 / 0:00", color = Dm, fontSize = 11.sp)
                }

                // Playback Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, null, tint = Tx) }
                    IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, null, tint = Tx) }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Tx),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Bg, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, null, tint = Tx) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Repeat, null, tint = Tx) }
                }

                // Waveform Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(P2)
                ) {
                    // Waveform Canvas goes here
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom Sheet (Simulated)
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    containerColor = Pn,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Ln) }
                ) {
                    TabContent(tab = selectedTab)
                }
            }
        }
    }
}

@Composable
fun TopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Bg)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("AUDIOVIS PRO", color = Tx, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(Ln)) // Beat dot
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {}) { Icon(Icons.Default.Undo, null, tint = Tx) }
        IconButton(onClick = {}) { Icon(Icons.Default.Redo, null, tint = Tx) }
        IconButton(onClick = {}) { Icon(Icons.Default.Menu, null, tint = Tx) }
        IconButton(onClick = {}) { Icon(Icons.Default.Settings, null, tint = Tx) }
    }
}

@Composable
fun BottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        "Media" to Icons.Default.LibraryMusic,
        "Layers" to Icons.Default.Layers,
        "Presets" to Icons.Default.Favorite,
        "Audio" to Icons.Default.GraphicEq,
        "Scene" to Icons.Default.Image,
        "Export" to Icons.Default.Share
    )

    Column {
        Divider(color = Ln, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Bg),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                val isSelected = selectedTab == index
                val color = if (isSelected) Ac else Dm
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabSelected(index) }
                ) {
                    Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(21.dp))
                    Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun TabContent(tab: Int) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth().height(300.dp)) {
        Text(
            text = when(tab) {
                0 -> "Media Library"
                1 -> "Layer Inspector"
                2 -> "Presets"
                3 -> "Audio Settings"
                4 -> "Scene Settings"
                5 -> "Export Options"
                else -> ""
            },
            color = Tx, fontSize = 14.sp, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Controls for this section would go here.", color = Dm, fontSize = 12.sp)
    }
}