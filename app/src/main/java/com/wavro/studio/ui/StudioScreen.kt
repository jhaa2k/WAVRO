package com.wavro.studio.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.wavro.studio.domain.LayerType
import com.wavro.studio.editor.StudioViewModel
import com.wavro.studio.rendering.AudioPreview
import com.wavro.studio.visualization.SpectrumMapper
import kotlinx.coroutines.flow.StateFlow

// Theme colors — matches the Claude Dark palette from styles.css
private val BG = Color(0xFF262624)
private val PN = Color(0xFF30302E)
private val LN = Color(0xFF454440)
private val TX = Color(0xFFFAF9F5)
private val DM = Color(0xFFB0AEA5)
private val AC = Color(0xFFD97757)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    vm: StudioViewModel,
    projectState: StateFlow<com.wavro.studio.domain.StudioProject>,
    playback: StateFlow<com.wavro.studio.audio.PlaybackState>,
    bands: StateFlow<SpectrumMapper.Bands>
) {
    val project by projectState.collectAsState()
    val play by playback.collectAsState()
    val b by bands.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.loadTrack(it, null) } }

    Scaffold(
        containerColor = BG,
        topBar = { TopBar(onOpen = { picker.launch(arrayOf("audio/*")) }) },
        bottomBar = {
            BottomNav(selectedTab) { idx ->
                selectedTab = idx
                showSheet = true
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                AudioPreview(layers = project.layers, bands = b)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PREVIEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${b.volume.times(100).toInt()}%", color = Color.White, fontSize = 9.sp)
                }
            }

            Text(
                text = play.currentTrackName ?: "No track loaded",
                color = DM, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                maxLines = 1
            )
            Text(
                text = "${play.positionMs / 1000}s / ${play.durationMs / 1000}s",
                color = DM, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, null, tint = TX) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, null, tint = TX) }
                Surface(
                    color = TX, shape = RoundedCornerShape(50),
                    modifier = Modifier.size(54.dp)
                ) {
                    IconButton(onClick = { vm.togglePlayback() }, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            if (play.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null, tint = BG
                        )
                    }
                }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, null, tint = TX) }
                IconButton(onClick = {}) { Icon(Icons.Default.Repeat, null, tint = TX) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PN)
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = PN,
            dragHandle = { BottomSheetDefaults.DragHandle(color = LN) }
        ) {
            SheetContent(selectedTab, vm, project)
        }
    }
}

@Composable
private fun TopBar(onOpen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).background(BG).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("WAVRO STUDIO", color = TX, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpen) { Icon(Icons.Default.LibraryMusic, null, tint = TX) }
        IconButton(onClick = {}) { Icon(Icons.Default.Undo, null, tint = TX) }
        IconButton(onClick = {}) { Icon(Icons.Default.Redo, null, tint = TX) }
        IconButton(onClick = {}) { Icon(Icons.Default.Settings, null, tint = TX) }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        "Media" to Icons.Default.LibraryMusic,
        "Layers" to Icons.Default.Layers,
        "Presets" to Icons.Default.Favorite,
        "Audio" to Icons.Default.GraphicEq,
        "Scene" to Icons.Default.Image,
        "Export" to Icons.Default.Share
    )
    Column {
        Divider(color = LN, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).background(BG),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, (label, icon) ->
                val c = if (selected == i) AC else DM
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(i) }
                ) {
                    Icon(icon, label, tint = c, modifier = Modifier.size(21.dp))
                    Text(label, color = c, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SheetContent(tab: Int, vm: StudioViewModel, project: com.wavro.studio.domain.StudioProject) {
    LazyColumn(modifier = Modifier.fillMaxWidth().height(340.dp).padding(16.dp)) {
        item {
            Text(
                text = when (tab) {
                    0 -> "Media Library"
                    1 -> "Layers"
                    2 -> "Presets"
                    3 -> "Audio Settings"
                    4 -> "Scene"
                    5 -> "Export"
                    else -> ""
                },
                color = TX, fontWeight = FontWeight.Bold, fontSize = 14.sp
            )
            Spacer(Modifier.height(12.dp))
        }
        if (tab == 1) {
            items(project.layers) { layer ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(layer.name, color = TX, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteLayer(layer.id) }) {
                        Icon(Icons.Default.Delete, null, tint = AC)
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(LayerType.BARS, LayerType.CIRCLE, LayerType.WAVE, LayerType.PARTICLES)
                        .forEach { t ->
                            Button(onClick = { vm.addLayer(t) }) { Text(t.name) }
                        }
                }
            }
        } else {
            items(1) {
                Text("Coming soon.", color = DM, fontSize = 12.sp)
            }
        }
    }
}