package com.wavro.studio.audio

import kotlin.math.max

enum class BandMapping { LINEAR, LOGARITHMIC }
enum class BandCombine { MAX, AVERAGE, WEIGHTED }

data class FrequencyBand(
    val id: String,
    val label: String,
    val lowHz: Float,
    val highHz: Float,
    val gain: Float = 1f,
    val smoothing: Float = 0.65f,
    val weight: Float = 1f,
    val attackMs: Float = 20f,
    val releaseMs: Float = 180f
) {
    init {
        require(lowHz >= 0f && highHz > lowHz)
        require(gain in 0f..2f && smoothing in 0f..0.99f)
        require(weight >= 0f && attackMs >= 0f && releaseMs >= 0f)
    }
}

data class BandValue(val id: String, val label: String, val value: Float, val peak: Float)

/** Frequency-domain band routing. A band can be routed to any visual layer by id. */
class BandSplitter(initialBands: List<FrequencyBand> = defaultBands()) {
    var bands: List<FrequencyBand> = initialBands
        set(value) { require(value.size in 1..32); field = value; states = FloatArray(value.size); peaks = FloatArray(value.size) }

    private var states = FloatArray(initialBands.size)
    private var peaks = FloatArray(initialBands.size)

    init { require(initialBands.size in 1..32) }

    fun reset() { states.fill(0f); peaks.fill(0f) }

    fun process(frame: SpectrumFrame, deltaSeconds: Float): List<BandValue> {
        val binWidth = frame.binWidthHz
        return bands.mapIndexed { index, band ->
            val start = (band.lowHz / binWidth).toInt().coerceIn(0, frame.magnitudes.lastIndex)
            val end = (band.highHz / binWidth).toInt().coerceIn(start + 1, frame.magnitudes.size)
            var energy = 0f
            for (i in start until end) energy += frame.magnitudes[i]
            energy = (energy / max(1, end - start) * band.gain).coerceIn(0f, 1f)
            val tau = if (energy > states[index]) band.attackMs else band.releaseMs
            val alpha = if (tau <= 0f) 0f else kotlin.math.exp(-deltaSeconds.coerceAtLeast(0f) / (tau / 1000f))
            states[index] = alpha * states[index] + (1f - alpha) * energy
            peaks[index] = max(states[index], peaks[index] - deltaSeconds.coerceAtLeast(0f) * 0.35f)
            BandValue(band.id, band.label, states[index], peaks[index])
        }
    }

    fun combine(values: List<BandValue>, ids: Set<String>, mode: BandCombine): Float {
        val selected = values.filter { it.id in ids }
        if (selected.isEmpty()) return 0f
        return when (mode) {
            BandCombine.MAX -> selected.maxOf { it.value }
            BandCombine.AVERAGE -> selected.map { it.value }.average().toFloat()
            BandCombine.WEIGHTED -> {
                val weights = selected.associate { v -> v.id to (bands.firstOrNull { it.id == v.id }?.weight ?: 1f) }
                val total = selected.sumOf { weights[it.id]?.toDouble() ?: 0.0 }.toFloat()
                if (total <= 0f) 0f else selected.sumOf { (it.value * (weights[it.id] ?: 1f)).toDouble() }.toFloat() / total
            }
        }
    }

    companion object {
        fun defaultBands() = listOf(
            FrequencyBand("bass", "Bass", 20f, 250f),
            FrequencyBand("mid", "Mid", 250f, 4000f),
            FrequencyBand("treble", "Treble", 4000f, 20000f)
        )
    }
}
