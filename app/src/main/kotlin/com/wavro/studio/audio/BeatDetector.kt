package com.wavro.studio.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class BeatAlgorithm { ENERGY_ONSET, SPECTRAL_FLUX, COMB_FILTER }

data class BeatConfig(
    val algorithm: BeatAlgorithm = BeatAlgorithm.SPECTRAL_FLUX,
    val sensitivity: Float = 0.55f,
    val cooldownMs: Int = 180,
    val thresholdMultiplier: Float = 1.55f,
    val historySeconds: Float = 2.0f,
    val minBpm: Int = 60,
    val maxBpm: Int = 200,
    val manualBpm: Float? = null
) {
    init {
        require(sensitivity in 0f..1f)
        require(cooldownMs in 50..500)
        require(thresholdMultiplier in 1f..5f)
        require(historySeconds in 0.5f..5f)
        require(minBpm in 40..240 && maxBpm in minBpm..240)
        require(manualBpm == null || manualBpm in 40f..240f)
    }
}

data class BeatFrame(
    val pulse: Float,
    val detected: Boolean,
    val bpm: Float,
    val beatIndex: Long,
    val downbeat: Boolean,
    val bass: Float,
    val mid: Float,
    val treble: Float
)

/** Onset detector fed only by the app's decoded PCM spectrum. */
class BeatDetector(var config: BeatConfig = BeatConfig()) {
    private val history = ArrayDeque<Float>()
    private val intervals = ArrayDeque<Long>()
    private var previous = FloatArray(0)
    private var lastBeatUs = Long.MIN_VALUE / 2
    private var beatIndex = 0L
    private var pulse = 0f
    private var bpmEstimate = 0f
    private var previousEnergy = 0f

    fun reset() {
        history.clear(); intervals.clear(); previous = FloatArray(0)
        lastBeatUs = Long.MIN_VALUE / 2; beatIndex = 0; pulse = 0f
        bpmEstimate = 0f; previousEnergy = 0f
    }

    fun process(frame: SpectrumFrame, deltaSeconds: Float): BeatFrame {
        val bins = frame.magnitudes
        if (previous.size != bins.size) previous = FloatArray(bins.size)
        val bass = bandAverage(bins, frame.binWidthHz, 20f, 250f)
        val mid = bandAverage(bins, frame.binWidthHz, 250f, 4000f)
        val treble = bandAverage(bins, frame.binWidthHz, 4000f, 20000f)
        val energy = (bass * 0.55f + mid * 0.30f + treble * 0.15f)
        var flux = 0f
        for (i in bins.indices) {
            flux += max(0f, bins[i] - previous[i])
            previous[i] = bins[i]
        }
        flux /= max(1, bins.size)
        val onset = when (config.algorithm) {
            BeatAlgorithm.ENERGY_ONSET -> max(0f, energy - previousEnergy)
            BeatAlgorithm.SPECTRAL_FLUX -> flux
            BeatAlgorithm.COMB_FILTER -> max(0f, bass - previousEnergy * 0.35f) + flux * 0.2f
        }
        previousEnergy = energy
        val historyLimit = max(8, (config.historySeconds / max(0.001f, deltaSeconds)).toInt())
        history.addLast(onset)
        while (history.size > historyLimit) history.removeFirst()
        val average = if (history.isEmpty()) 0f else history.average().toFloat()
        val threshold = average * config.thresholdMultiplier * (1.5f - config.sensitivity)
        val cooldownUs = config.cooldownMs * 1000L
        val detected = onset > max(0.002f, threshold) && frame.timestampUs - lastBeatUs >= cooldownUs
        if (detected) {
            if (lastBeatUs > 0L) {
                val interval = frame.timestampUs - lastBeatUs
                val candidate = 60_000_000f / interval.coerceAtLeast(1L)
                if (candidate in config.minBpm.toFloat()..config.maxBpm.toFloat()) {
                    intervals.addLast(interval)
                    while (intervals.size > 12) intervals.removeFirst()
                    val sorted = intervals.toList().sorted()
                    val median = sorted[sorted.size / 2]
                    bpmEstimate = 60_000_000f / median.coerceAtLeast(1L)
                    if (bpmEstimate < 90f) bpmEstimate *= 2f
                    if (bpmEstimate > 180f) bpmEstimate *= 0.5f
                }
            }
            lastBeatUs = frame.timestampUs
            beatIndex++
            pulse = 1f
        } else {
            pulse *= kotlin.math.exp(-deltaSeconds.coerceAtLeast(0f) * 5.5f)
        }
        val bpm = config.manualBpm ?: bpmEstimate
        return BeatFrame(pulse.coerceIn(0f, 1f), detected, bpm, beatIndex, detected && beatIndex % 4L == 1L, bass, mid, treble)
    }

    private fun bandAverage(values: FloatArray, binWidth: Float, low: Float, high: Float): Float {
        val start = (low / binWidth).toInt().coerceIn(0, values.lastIndex)
        val end = (high / binWidth).toInt().coerceIn(start + 1, values.size)
        var total = 0f
        for (i in start until end) total += values[i]
        return total / max(1, end - start)
    }
}
