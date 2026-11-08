package com.wavro.studio.domain

import java.util.UUID

enum class ElementType(val title: String, val description: String) {
    BARS("Spectrum bars", "Audio-reactive frequency bars"),
    RADIAL("Radial ring", "A circular audio pulse"),
    PARTICLES("Particles", "Floating ambient particles"),
    TITLE("Track title", "Show the current track title"),
}

data class ProjectElement(
    val id: String = UUID.randomUUID().toString(),
    val type: ElementType,
    val enabled: Boolean = true,
)

data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Project",
    val backgroundColor: Long = 0xFF0D0D12,
    val elements: List<ProjectElement> = defaultElements(),
)

fun defaultElements() = listOf(
    ProjectElement(type = ElementType.PARTICLES),
    ProjectElement(type = ElementType.RADIAL),
    ProjectElement(type = ElementType.BARS),
    ProjectElement(type = ElementType.TITLE),
)
