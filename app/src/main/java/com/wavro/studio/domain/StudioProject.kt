package com.wavro.studio.domain

import java.util.UUID

enum class LayerType { BARS, CIRCLE, WAVE, DOTS, PARTICLES, TEXT, BACKGROUND, IMAGE, SHAPE, CONCENTRIC }

data class ReactiveRule(
    val id: String = UUID.randomUUID().toString(),
    val source: String = "bass",        // bass, volume, sub, mid, upmid, brill
    val property: String = "sy",        // sy, sx, opacity, glow, x, y
    val inputMin: Float = 0f,
    val inputMax: Float = 1f,
    val outputMin: Float = 100f,
    val outputMax: Float = 140f,
    val enabled: Boolean = true
)

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val type: LayerType = LayerType.BARS,
    val name: String = "Bars",
    val x: Float = 50f,
    val y: Float = 78f,
    val scaleX: Float = 100f,
    val scaleY: Float = 100f,
    val rotation: Float = 0f,
    val opacity: Float = 100f,
    val colorPrimary: Int = 0xFFFFFFFF.toInt(),
    val colorSecondary: Int = 0xFF6A6A75.toInt(),
    val barCount: Int = 48,
    val barThickness: Float = 70f,
    val barLength: Float = 100f,
    val cornerRadius: Float = 0f,
    val glow: Float = 0f,
    val reactiveRules: List<ReactiveRule> = emptyList(),
    val visible: Boolean = true,
    val locked: Boolean = false
)

data class SceneSettings(
    val backgroundColor: Int = 0xFF000000.toInt(),
    val cameraZoom: Float = 0f,
    val vignette: Float = 0f,
    val scanlines: Float = 0f,
    val grain: Float = 0f,
    val beatFlash: Float = 0f
)

data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled",
    val layers: List<Layer> = listOf(
        Layer(type = LayerType.BACKGROUND, name = "Background", colorPrimary = 0xFF000000.toInt()),
        Layer(type = LayerType.BARS, name = "Bars")
    ),
    val scene: SceneSettings = SceneSettings(),
    val lastModified: Long = System.currentTimeMillis()
)