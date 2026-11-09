package com.wavro.studio.domain

import java.util.UUID

// All available element types in the visualizer
enum class LayerType {
    BARS, CIRCLE, WAVE, DOTS, TEXT, BACKGROUND, IMAGE, SHAPE, PARTICLES
}

// Text-specific properties
data class TextProperties(
    val text: String = "WAVRO STUDIO",
    val textSize: Float = 32f,
    val isBold: Boolean = true,
    val fontFamily: String = "sans-serif", // Default Android font
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 1f
)

// Spectrum/Visualizer-specific properties
data class SpectrumProperties(
    val barCount: Int = 48,
    val sensitivity: Float = 1.0f,
    val minFrequency: Float = 20f,
    val maxFrequency: Float = 20000f,
    val smoothing: Float = 0.5f,
    val roundedCaps: Boolean = false
)

// The core Layer model (every element you add to the screen)
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val type: LayerType = LayerType.BARS,
    val name: String = "Layer",
    
    // --- Transform Properties ---
    val x: Float = 50f,            // 0-100% of screen width
    val y: Float = 50f,            // 0-100% of screen height
    val scale: Float = 100f,       // 10-300%
    val rotation: Float = 0f,      // 0-360 degrees
    val opacity: Float = 100f,     // 0-100%
    val blendMode: String = "src_over", // src_over, screen, multiply, etc.
    
    // --- Appearance Properties ---
    val colorPrimary: Int = 0xFFFFFFFF.toInt(),   // Hex color
    val colorSecondary: Int = 0xFFD97757.toInt(), // Secondary color for gradients
    val useGradient: Boolean = false,
    val glowRadius: Float = 0f,
    val shadowOffset: Float = 0f,
    
    // --- Specific Data ---
    val spectrum: SpectrumProperties = SpectrumProperties(),
    val text: TextProperties = TextProperties(),
    
    // --- Layer Management ---
    val visible: Boolean = true,
    val locked: Boolean = false
)

// Global Scene Settings (applies to the whole canvas)
data class SceneSettings(
    val backgroundColor: Int = 0xFF000000.toInt(), // Black
    val cameraZoom: Float = 0f,      // 0-100%
    val vignette: Float = 0f,        // 0-100%
    val scanlines: Float = 0f,       // 0-100%
    val filmGrain: Float = 0f,       // 0-100%
    val beatFlash: Float = 0f,       // 0-100%
    val hueJumpPerBeat: Float = 0f   // 0-90 degrees
)

// The main Project (what gets saved/loaded)
data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "My Visualizer",
    val layers: List<Layer> = listOf(
        // Default starting layers
        Layer(type = LayerType.BARS, name = "Spectrum", y = 70f),
        Layer(type = LayerType.TEXT, name = "Title", text = TextProperties(text = "WAVRO"), y = 30f)
    ),
    val scene: SceneSettings = SceneSettings(),
    val lastModified: Long = System.currentTimeMillis()
)