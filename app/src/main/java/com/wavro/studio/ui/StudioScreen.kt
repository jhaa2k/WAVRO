package com.wavro.studio.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wavro.studio.audio.PlaybackState
import com.wavro.studio.domain.ElementType
import com.wavro.studio.domain.ProjectElement
import com.wavro.studio.domain.StudioProject
import com.wavro.studio.editor.StudioViewModel
import com.wavro.studio.rendering.AudioPreview

private val Background = Color(0xFF262624)
private val Panel = Color(0xFF30302E)
private val Line = Color(0xFF454440)
private val TextPrimary = Color(0xFFFAF9F5)
private val Muted = Color(0xFFB0AEA5)
private val Accent = Color(0xFFD97757)

private enum class StudioTab(val label: String) {
    MEDIA("Media"), LAYERS("Layers"), PRESETS("Presets"), AUDIO("Audio"), SCENE("Scene"), EXPORT("Export"),
}

@Composable
fun StudioScreen(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()
    val playback by viewModel.playback.collectAsState()
    var selectedTab by remember { mutableIntStateOf(-1) }
    var showElementPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.refreshVisualization()
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some document providers grant only temporary read access.
            }
            viewModel.openAudio(uri)
        }
    }

    if (showElementPicker) {
        ElementPicker(onDismiss = { showElementPicker = false }, onSelect = {
            viewModel.addElement(it)
            showElementPicker = false
        })
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = Panel,
            title = { Text("Studio settings", color = TextPrimary) },
            text = { Text("Preview: 1920 × 1080 · 60 FPS\nExport and rendering settings will be available in the Export panel.", color = Muted) },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done", color = Accent) } },
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Background),
    ) {
        val isWide = maxWidth > 820.dp
        Column(Modifier.fillMaxSize()) {
            Header(
                isLive = playback.isPlaying,
                onSettings = { showSettings = true },
                onPanelToggle = { selectedTab = if (selectedTab == -1) StudioTab.LAYERS.ordinal else -1 },
            )
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    PreviewStage(project, playback, Modifier.weight(1f).fillMaxWidth())
                    Transport(
                        playback = playback,
                        onToggle = viewModel::togglePlayback,
                        onSeek = viewModel::seekTo,
                    )
                    Waveform(playback, viewModel::seekTo)
                    if (isWide) {
                        BottomNavigation(selectedTab) { selectedTab = it }
                    }
                }
                if (isWide && selectedTab >= 0) {
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Line)
                    Column(Modifier.width(380.dp).fillMaxHeight().background(Panel)) {
                        PanelHeader(StudioTab.entries[selectedTab].label) { selectedTab = -1 }
                        TabPanel(
                            tab = StudioTab.entries[selectedTab],
                            project = project,
                            playback = playback,
                            savedProjects = savedProjects,
                            viewModel = viewModel,
                            onOpenFile = { filePicker.launch(arrayOf("audio/*")) },
                            onAddElement = { showElementPicker = true },
                            onRequestAudioPermission = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.refreshVisualization()
                                } else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                            },
                        )
                    }
                }
            }
            if (!isWide) BottomNavigation(selectedTab) { index ->
                selectedTab = if (selectedTab == index) -1 else index
            }
        }

        if (!isWide && selectedTab >= 0) {
            ModalBottomSheet(
                onDismissRequest = { selectedTab = -1 },
                containerColor = Panel,
                contentColor = TextPrimary,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Line) },
            ) {
                TabPanel(
                    tab = StudioTab.entries[selectedTab],
                    project = project,
                    playback = playback,
                    savedProjects = savedProjects,
                    viewModel = viewModel,
                    onOpenFile = { filePicker.launch(arrayOf("audio/*")) },
                    onAddElement = { showElementPicker = true },
                    onRequestAudioPermission = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.refreshVisualization()
                        } else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )
            }
        }
    }
}

@Composable
private fun Header(isLive: Boolean, onSettings: () -> Unit, onPanelToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).background(Background).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("AUDIOVIS PRO", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
        Spacer(Modifier.width(9.dp))
        Box(Modifier.size(7.dp).background(if (isLive) Accent else Line, CircleShape))
        Spacer(Modifier.weight(1f))
        HeaderIcon(Icons.Default.Undo, "Undo", enabled = false) {}
        HeaderIcon(Icons.Default.Redo, "Redo", enabled = false) {}
        HeaderIcon(Icons.Default.Menu, "Toggle panel", onClick = onPanelToggle)
        HeaderIcon(Icons.Default.Settings, "Settings", onClick = onSettings)
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = description, tint = if (enabled) TextPrimary else Muted.copy(alpha = 0.45f), modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun PreviewStage(project: StudioProject, playback: PlaybackState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(10.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black),
    ) {
        AudioPreview(project, playback, Modifier.fillMaxSize())
        Row(
            Modifier.align(Alignment.TopStart).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HudPill("PREVIEW")
            HudPill("1920 × 1080")
            HudPill("60 FPS")
        }
    }
}

@Composable
private fun HudPill(label: String) {
    Text(label, color = TextPrimary.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 4.dp))
}

@Composable
private fun Transport(playback: PlaybackState, onToggle: () -> Unit, onSeek: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(playback.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatTime(playback.positionMs)} / ${formatTime(playback.durationMs)}", color = Muted, fontSize = 10.sp)
        }
        IconButton(onClick = {}, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Shuffle, "Shuffle", tint = Muted, modifier = Modifier.size(17.dp)) }
        IconButton(onClick = {}, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", tint = Muted, modifier = Modifier.size(20.dp)) }
        Button(
            onClick = onToggle,
            enabled = !playback.isLoading && (playback.isPlaying || playback.title != "No track selected"),
            modifier = Modifier.size(46.dp), shape = CircleShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Background, disabledContainerColor = TextPrimary.copy(alpha = 0.45f)),
        ) {
            Icon(if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", modifier = Modifier.size(25.dp))
        }
        IconButton(onClick = {}, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.SkipNext, "Next", tint = Muted, modifier = Modifier.size(20.dp)) }
        IconButton(onClick = {}, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Repeat, "Repeat", tint = Muted, modifier = Modifier.size(17.dp)) }
    }
}

@Composable
private fun Waveform(playback: PlaybackState, onSeek: (Long) -> Unit) {
    val progress = if (playback.durationMs > 0) (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f) else 0f
    Box(
        Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(6.dp))
            .background(Panel).clickable(enabled = playback.durationMs > 0) { onSeek((playback.durationMs * 0.5f).toLong()) },
    ) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 6.dp)) {
            val centerY = size.height / 2f
            val count = 88
            val gap = 2.dp.toPx()
            val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
            repeat(count) { index ->
                val normalized = index.toFloat() / count
                val bin = playback.spectrum.getOrNull(index * playback.spectrum.size / count) ?: 0f
                val pseudoWave = 0.12f + ((index * 37 % 29) / 29f) * 0.38f
                val amplitude = (if (playback.isPlaying) bin.coerceAtLeast(pseudoWave) else pseudoWave).coerceIn(0.08f, 0.85f)
                val height = size.height * amplitude
                val color = if (normalized <= progress) Accent else Muted.copy(alpha = 0.55f)
                drawLine(color, androidx.compose.ui.geometry.Offset(index * (barWidth + gap) + barWidth / 2f, centerY - height / 2f), androidx.compose.ui.geometry.Offset(index * (barWidth + gap) + barWidth / 2f, centerY + height / 2f), barWidth, StrokeCap.Round)
            }
            drawLine(TextPrimary, androidx.compose.ui.geometry.Offset(size.width * progress, 0f), androidx.compose.ui.geometry.Offset(size.width * progress, size.height), 1.dp.toPx())
        }
    }
}

@Composable
private fun BottomNavigation(selectedTab: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Media", Icons.Default.LibraryMusic, StudioTab.MEDIA),
        Triple("Layers", Icons.Default.Layers, StudioTab.LAYERS),
        Triple("Presets", Icons.Default.Favorite, StudioTab.PRESETS),
        Triple("Audio", Icons.Default.GraphicEq, StudioTab.AUDIO),
        Triple("Scene", Icons.Default.Image, StudioTab.SCENE),
        Triple("Export", Icons.Default.Share, StudioTab.EXPORT),
    )
    Column {
        Divider(color = Line, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(54.dp).background(Background), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, (label, icon, _) ->
                val selected = selectedTab == index
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, label, tint = if (selected) Accent else Muted, modifier = Modifier.size(19.dp))
                    Text(label, color = if (selected) Accent else Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onClose) { Text("CLOSE", color = Muted, fontSize = 9.sp) }
    }
    Divider(color = Line)
}

@Composable
private fun TabPanel(
    tab: StudioTab,
    project: StudioProject,
    playback: PlaybackState,
    savedProjects: List<StudioProject>,
    viewModel: StudioViewModel,
    onOpenFile: () -> Unit,
    onAddElement: () -> Unit,
    onRequestAudioPermission: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        when (tab) {
            StudioTab.MEDIA -> {
                SectionTitle("AUDIO SOURCE", if (playback.isPlaying) "PLAYING" else "READY")
                Spacer(Modifier.height(12.dp))
                PanelCard {
                    Text(playback.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(playback.artist, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onOpenFile, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(8.dp)) { Text("Open audio file", color = Background) }
                    OutlinedButton(onClick = viewModel::startDemo, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("Play built-in demo", color = TextPrimary) }
                    playback.error?.let { Text(it, color = Accent, fontSize = 11.sp) }
                }
            }
            StudioTab.LAYERS -> ElementsPanel(project.elements, onAddElement, viewModel::setElementEnabled, viewModel::removeElement)
            StudioTab.PRESETS -> {
                SectionTitle("STARTING POINTS", "PRESETS")
                Spacer(Modifier.height(12.dp))
                listOf("Spectrum Pulse", "Minimal Wave", "Orbit Rings").forEach { preset ->
                    PanelCard(Modifier.padding(bottom = 8.dp)) {
                        Text(preset, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Preset browser will be connected to the native scene model.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            StudioTab.AUDIO -> {
                SectionTitle("LIVE ANALYSIS", if (playback.visualizationAvailable) "ACTIVE" else "PERMISSION")
                Spacer(Modifier.height(12.dp))
                PanelCard {
                    Text("Audio visualizer", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Allow Android audio visualization to drive the preview spectrum.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    Button(onClick = onRequestAudioPermission, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Enable audio analysis", color = Background) }
                }
            }
            StudioTab.SCENE -> {
                SectionTitle("SCENE", "${project.elements.size} ELEMENTS")
                Spacer(Modifier.height(12.dp))
                PanelCard {
                    Text("${project.name}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Background · ${project.backgroundColor.toULong().toString(16).uppercase()}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            StudioTab.EXPORT -> {
                SectionTitle("EXPORT SETTINGS", "VIDEO")
                Spacer(Modifier.height(12.dp))
                PanelCard {
                    Text("1920 × 1080 · 60 FPS", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("MP4 · 12 Mbps", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Export engine · Stage 4", color = Background) }
                }
            }
        }
    }
}

@Composable
private fun PanelCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Background)) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun ElementsPanel(elements: List<ProjectElement>, onAdd: () -> Unit, onToggle: (String, Boolean) -> Unit, onRemove: (String) -> Unit) {
    SectionTitle("SCENE ELEMENTS", "${elements.size} TOTAL")
    Spacer(Modifier.height(10.dp))
    elements.forEach { element ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(if (element.enabled) Accent else Muted, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(element.type.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(element.type.description, color = Muted, fontSize = 10.sp)
            }
            Switch(checked = element.enabled, onCheckedChange = { onToggle(element.id, it) })
            TextButton(onClick = { onRemove(element.id) }) { Text("Remove", color = Muted, fontSize = 10.sp) }
        }
        Divider(color = Line.copy(alpha = 0.7f))
    }
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
        Text("+  Add element", color = Background)
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Spacer(Modifier.weight(1f))
        Text(trailing, color = Muted, fontSize = 9.sp, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun ElementPicker(onDismiss: () -> Unit, onSelect: (ElementType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to your scene", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ElementType.entries.forEach { type ->
                    TextButton(onClick = { onSelect(type) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(type.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(type.description, color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Accent) } },
        containerColor = Panel,
    )
}

private fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "0:00"
    val totalSeconds = timeMs / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
