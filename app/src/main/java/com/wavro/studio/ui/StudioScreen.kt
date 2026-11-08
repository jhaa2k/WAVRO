package com.wavro.studio.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val Background = Color(0xFF0D0D12)
private val Panel = Color(0xFF17171E)
private val Muted = Color(0xFF96949F)
private val Coral = Color(0xFFFF9A6B)

private enum class StudioTab(val label: String) {
    STUDIO("Studio"),
    ELEMENTS("Elements"),
    PROJECTS("Projects"),
}

@Composable
fun StudioScreen(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()
    val playback by viewModel.playback.collectAsState()
    var selectedTab by remember { mutableStateOf(StudioTab.STUDIO) }
    var showElementPicker by remember { mutableStateOf(false) }

    val audioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.refreshVisualization()
        }
    }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // The selected provider may grant temporary access only; the track still opens now.
            }
            viewModel.openAudio(uri)
        }
    }

    if (showElementPicker) {
        ElementPicker(
            onDismiss = { showElementPicker = false },
            onSelect = {
                viewModel.addElement(it)
                showElementPicker = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Header(project)
        Column(Modifier.padding(horizontal = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StudioTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                    )
                }
            }

            if (selectedTab == StudioTab.STUDIO) {
                PreviewCard(project, playback)
                Spacer(Modifier.height(16.dp))
                TrackCard(
                    playback = playback,
                    onToggle = viewModel::togglePlayback,
                    onOpen = { filePicker.launch(arrayOf("audio/*")) },
                    onDemo = viewModel::startDemo,
                    onSeek = viewModel::seekTo,
                )
                if (!playback.visualizationAvailable) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.refreshVisualization()
                            } else {
                                audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF34333B)),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Enable live audio analysis", color = Color.White)
                    }
                    Text(
                        "WAVRO uses Android's audio visualizer permission to analyze playback. Audio is not recorded or uploaded.",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
                    )
                }
                playback.error?.let {
                    Text(it, color = Coral, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(18.dp))
                SectionTitle("YOUR SCENE", "${project.elements.count { it.enabled }} ACTIVE")
                project.elements.take(4).forEach { element ->
                    SceneElementRow(element, onToggle = { enabled ->
                        viewModel.setElementEnabled(element.id, enabled)
                    })
                }
                TextButton(onClick = { selectedTab = StudioTab.ELEMENTS }) {
                    Text("Edit all elements", color = Coral)
                }
            } else if (selectedTab == StudioTab.ELEMENTS) {
                ElementsPanel(
                    elements = project.elements,
                    onAdd = { showElementPicker = true },
                    onToggle = viewModel::setElementEnabled,
                    onRemove = viewModel::removeElement,
                )
            } else {
                ProjectsPanel(
                    project = project,
                    projects = savedProjects,
                    onNameChange = viewModel::updateProjectName,
                    onSave = viewModel::saveProject,
                    onNew = viewModel::newProject,
                    onSelect = viewModel::selectProject,
                    onDelete = viewModel::deleteProject,
                )
            }
        }
    }
}

@Composable
private fun Header(project: StudioProject) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = Color(0xFF292528), shape = RoundedCornerShape(12.dp)) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Text("W", color = Coral, fontSize = 21.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("WAVRO", fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text("AUDIO VISUAL STUDIO", color = Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
        }
        Text(
            project.name,
            color = Muted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(125.dp),
        )
    }
}

@Composable
private fun PreviewCard(project: StudioProject, playback: PlaybackState) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Column {
            AudioPreview(
                project = project,
                playback = playback,
                modifier = Modifier.fillMaxWidth().height(300.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        playback.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(playback.artist, color = Muted, fontSize = 12.sp, maxLines = 1)
                }
                Text("1920 × 1080", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TrackCard(
    playback: PlaybackState,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDemo: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onToggle,
                    enabled = !playback.isLoading &&
                        (playback.isPlaying || playback.title != "No track selected"),
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral),
                ) {
                    Text(
                        when {
                            playback.isLoading -> "…"
                            playback.isPlaying -> "Ⅱ"
                            else -> "▶"
                        },
                        color = Background,
                        fontSize = 19.sp,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (playback.isPlaying) "NOW PLAYING" else "AUDIO SOURCE", color = Muted, fontSize = 9.sp, letterSpacing = 1.3.sp)
                    Text(playback.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onOpen) { Text("OPEN", color = Coral, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }

            Slider(
                value = if (playback.durationMs > 0) {
                    (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
                } else {
                    0f
                },
                onValueChange = { fraction ->
                    if (playback.durationMs > 0) onSeek((playback.durationMs * fraction).toLong())
                },
                enabled = playback.durationMs > 0,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(playback.positionMs), color = Muted, fontSize = 10.sp)
                Text(formatTime(playback.durationMs), color = Muted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDemo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF37353E)),
            ) {
                Text("Play built-in demo", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ElementsPanel(
    elements: List<ProjectElement>,
    onAdd: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    SectionTitle("SCENE ELEMENTS", "${elements.size} TOTAL")
    Spacer(Modifier.height(12.dp))
    elements.forEach { element ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(17.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = Color(0xFF29252A),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(element.type.title.take(1), color = Coral, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(element.type.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(element.type.description, color = Muted, fontSize = 11.sp)
                }
                Switch(
                    checked = element.enabled,
                    onCheckedChange = { onToggle(element.id, it) },
                )
                TextButton(onClick = { onRemove(element.id) }) {
                    Text("Remove", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
    Button(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text("+  Add element")
    }
}

@Composable
private fun ProjectsPanel(
    project: StudioProject,
    projects: List<StudioProject>,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onNew: () -> Unit,
    onSelect: (StudioProject) -> Unit,
    onDelete: (String) -> Unit,
) {
    SectionTitle("PROJECT LIBRARY", "${projects.size} SAVED")
    Spacer(Modifier.height(12.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("CURRENT PROJECT", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            TextField(
                value = project.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Project name") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF202027),
                    unfocusedContainerColor = Color(0xFF202027),
                    focusedIndicatorColor = Coral,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Save project")
                }
                OutlinedButton(onClick = onNew, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("New")
                }
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    projects.forEach { saved ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text(saved.name, fontWeight = FontWeight.SemiBold)
                    Text("${saved.elements.size} elements", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = { onSelect(saved) }) { Text("Open", color = Coral) }
                TextButton(onClick = { onDelete(saved.id) }) { Text("Delete", color = Muted, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun SceneElementRow(element: ProjectElement, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(7.dp)
                .background(if (element.enabled) Coral else Muted, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(element.type.title, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = element.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Spacer(Modifier.weight(1f))
        Text(trailing, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun ElementPicker(onDismiss: () -> Unit, onSelect: (ElementType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to your scene") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ElementType.entries.forEach { type ->
                    TextButton(
                        onClick = { onSelect(type) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(type.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(type.description, color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Panel,
    )
}

private fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "0:00"
    val totalSeconds = timeMs / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
