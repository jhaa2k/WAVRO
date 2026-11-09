package com.wavro.studio.domain

import java.util.UUID

enum class LayerType { 
    BARS, CIRCLE, WAVE, DOTS, PARTICLES, TEXT, BACKGROUND, IMAGE, SHAPE, CONCENTRIC 
}

data class TextLayerProperties(
    val text: String = "AUDIOVIS",
    val fontFamily: String = "sans-serif",
    val textSize: Float = 24f,
    val isBold: Boolean = true,
    val color: Int = 0xFFFFFFFF.toInt()
)

data class SpectrumProperties(
    val barCount: Int = 48,
    val minFrequency: Float = 20f,
    val maxFrequency: Float = 20000f,
    val smoothing: Float = 0.5f,
    val sensitivity: Float = 1.0f
)

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val type: LayerType = LayerType.BARS,
    val name: String = "Layer",
    
    // Transform
    val x: Float = 50f,
    val y: Float = 50f,
    val scaleX: Float = 100f,
    val scaleY: Float = 100f,
    val rotation: Float = 0f,
    val opacity: Float = 100f,
    
    // Appearance
    val colorPrimary: Int = 0xFFFFFFFF.toInt(),
    val colorSecondary: Int = 0xFF000000.toInt(),
    
    // Specific Properties
    val spectrum: SpectrumProperties = SpectrumProperties(),
    val text: TextLayerProperties = TextLayerProperties(),
    
    val visible: Boolean = true,
    val locked: Boolean = false
)

data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Project",
    val layers: List<Layer> = listOf(
        Layer(type = LayerType.BACKGROUND, name = "Background", colorPrimary = 0xFF000000.toInt()),
        Layer(type = LayerType.BARS, name = "Spectrum")
    ),
    val lastModified: Long = System.currentTimeMillis()
)