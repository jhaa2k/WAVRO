package com.wavro.studio.domain

import java.util.UUID

enum class LayerType { 
    BARS, CIRCLE, WAVE, DOTS, TEXT, BACKGROUND 
}

data class TextProperties(
    val text: String = "WAVRO",
    val textSize: Float = 32f,
    val isBold: Boolean = true
)

data class SpectrumProperties(
    val barCount: Int = 48,
    val sensitivity: Float = 1.0f
)

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val type: LayerType = LayerType.BARS,
    val name: String = "Layer",
    
    // Transform (0-100 represents percentage of screen)
    val x: Float = 50f,
    val y: Float = 50f,
    val scale: Float = 100f,
    val rotation: Float = 0f,
    val opacity: Float = 100f,
    
    // Appearance
    val colorPrimary: Int = 0xFFFFFFFF.toInt(),
    
    // Specific Data
    val spectrum: SpectrumProperties = SpectrumProperties(),
    val text: TextProperties = TextProperties(),
    
    val visible: Boolean = true
)

data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "My Project",
    val layers: List<Layer> = listOf(
        Layer(type = LayerType.BACKGROUND, name = "Background", colorPrimary = 0xFF000000.toInt()),
        Layer(type = LayerType.BARS, name = "Spectrum")
    )
)