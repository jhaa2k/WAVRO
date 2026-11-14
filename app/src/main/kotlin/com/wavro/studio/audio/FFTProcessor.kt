package com.wavro.studio.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

enum class FftWindow { HANN, HAMMING, BLACKMAN, RECTANGULAR, BLACKMAN_HARRIS }
enum class SpectrumNormalization { NONE, PEAK, RMS }

data class FftConfig(
    val size: Int = 2048,
    val window: FftWindow = FftWindow.HANN,
    val overlapPercent: Int = 50,
    val attackMs: Float = 18f,
    val releaseMs: Float = 180f,
    val normalization: SpectrumNormalization = SpectrumNormalization.PEAK,
    val useDecibels: Boolean = false
) {
    init {
        require(size in setOf(512, 1024, 2048, 4096, 8192)) { "FFT size must be 512, 1024, 2048, 4096 or 8192" }
        require(overlapPercent in setOf(0, 25, 50, 75))
        require(attackMs >= 0f && releaseMs >= 0f)
    }
}

data class SpectrumFrame(
    val magnitudes: FloatArray,
    val phases: FloatArray,
    val sampleRate: Int,
    val fftSize: Int,
    val timestampUs: Long
) {
    val binWidthHz: Float get() = sampleRate.toFloat() / fftSize
}

/** Allocation-conscious radix-2 FFT for decoded, normalized PCM samples. Not an audio-capture API. */
class FFTProcessor(config: FftConfig = FftConfig()) {
    var config: FftConfig = config
        set(value) { field = value; rebuild(value) }

    private var real = FloatArray(config.size)
    private var imag = FloatArray(config.size)
    private var window = FloatArray(config.size)
    private var smoothed = FloatArray(config.size / 2)
    private var bitReverse = IntArray(config.size)
    private var previousSampleRate = 0

    init { rebuild(config) }

    fun reset() {
        real.fill(0f); imag.fill(0f); smoothed.fill(0f)
        previousSampleRate = 0
    }

    fun process(
        pcm: FloatArray,
        sampleRate: Int,
        timestampUs: Long,
        offset: Int = 0,
        deltaSeconds: Float = config.size.toFloat() / max(1, sampleRate)
    ): SpectrumFrame {
        require(sampleRate > 0) { "Sample rate must be positive" }
        require(offset >= 0 && offset + config.size <= pcm.size) { "PCM frame is shorter than configured FFT size" }
        if (previousSampleRate != sampleRate) { smoothed.fill(0f); previousSampleRate = sampleRate }

        var rms = 0.0
        for (i in 0 until config.size) {
            val sample = pcm[offset + i].coerceIn(-1f, 1f)
            rms += sample * sample
            real[i] = sample * window[i]
            imag[i] = 0f
        }
        val inputRms = sqrt(rms / config.size).toFloat()
        transform()

        val bins = config.size / 2
        var peak = 1e-9f
        var sumSquares = 0.0
        for (i in 0 until bins) {
            val magnitude = sqrt(real[i] * real[i] + imag[i] * imag[i]) * (2f / config.size)
            real[i] = magnitude
            peak = max(peak, magnitude)
            sumSquares += magnitude * magnitude
        }
        val rmsMagnitude = sqrt(sumSquares / max(1, bins)).toFloat()
        val divisor = when (config.normalization) {
            SpectrumNormalization.NONE -> 1f
            SpectrumNormalization.PEAK -> peak
            SpectrumNormalization.RMS -> max(1e-6f, rmsMagnitude)
        }
        val attack = timeCoefficient(config.attackMs, deltaSeconds)
        val release = timeCoefficient(config.releaseMs, deltaSeconds)
        val out = FloatArray(bins)
        val phase = FloatArray(bins)
        for (i in 0 until bins) {
            var value = (real[i] / divisor).coerceIn(0f, 1f)
            if (config.useDecibels) value = ((20f * log10(max(1e-7f, value)) + 80f) / 80f).coerceIn(0f, 1f)
            val coefficient = if (value > smoothed[i]) attack else release
            smoothed[i] = coefficient * smoothed[i] + (1f - coefficient) * value
            out[i] = smoothed[i]
            phase[i] = kotlin.math.atan2(imag[i], real[i])
        }
        // Keep the original RMS available as bin zero's companion only through the frame's metadata contract.
        if (out.isNotEmpty() && config.normalization == SpectrumNormalization.NONE) out[0] = max(out[0], inputRms)
        return SpectrumFrame(out, phase, sampleRate, config.size, timestampUs)
    }

    private fun timeCoefficient(milliseconds: Float, dt: Float): Float {
        if (milliseconds <= 0f) return 0f
        return kotlin.math.exp(-dt.coerceAtLeast(0f) / (milliseconds / 1000f))
    }

    private fun rebuild(c: FftConfig) {
        real = FloatArray(c.size); imag = FloatArray(c.size)
        smoothed = FloatArray(c.size / 2); window = FloatArray(c.size)
        bitReverse = IntArray(c.size)
        val bits = Integer.numberOfTrailingZeros(c.size)
        for (i in 0 until c.size) bitReverse[i] = Integer.reverse(i) ushr (32 - bits)
        for (i in 0 until c.size) {
            val x = 2.0 * PI * i / (c.size - 1)
            window[i] = when (c.window) {
                FftWindow.RECTANGULAR -> 1f
                FftWindow.HANN -> (0.5 - 0.5 * cos(x)).toFloat()
                FftWindow.HAMMING -> (0.54 - 0.46 * cos(x)).toFloat()
                FftWindow.BLACKMAN -> (0.42 - 0.5 * cos(x) + 0.08 * cos(2 * x)).toFloat()
                FftWindow.BLACKMAN_HARRIS -> (0.35875 - 0.48829 * cos(x) + 0.14128 * cos(2 * x) - 0.01168 * cos(3 * x)).toFloat()
            }
        }
    }

    private fun transform() {
        val n = real.size
        for (i in 0 until n) {
            val j = bitReverse[i]
            if (j > i) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }
        var length = 2
        while (length <= n) {
            val half = length / 2
            val angle = (-2.0 * PI / length)
            val stepR = cos(angle).toFloat()
            val stepI = kotlin.math.sin(angle).toFloat()
            var base = 0
            while (base < n) {
                var wr = 1f; var wi = 0f
                for (j in 0 until half) {
                    val even = base + j
                    val odd = even + half
                    val tr = wr * real[odd] - wi * imag[odd]
                    val ti = wr * imag[odd] + wi * real[odd]
                    real[odd] = real[even] - tr; imag[odd] = imag[even] - ti
                    real[even] += tr; imag[even] += ti
                    val nextWr = wr * stepR - wi * stepI
                    wi = wr * stepI + wi * stepR; wr = nextWr
                }
                base += length
            }
            length = length shl 1
        }
    }
}
