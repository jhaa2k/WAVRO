package com.wavro.studio.visualization

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Converts raw FFT bytes into normalized frequency band values (0..1).
 * Bands match the JS implementation: sub, bass, lowmid, mid, upmid, presence, brill.
 */
class SpectrumMapper(sampleRate: Int = 44100, fftSize: Int = 1024) {

    private val binHz: Float = (sampleRate / 2f) / (fftSize / 2f)

    data class Bands(
        val sub: Float = 0f,
        val bass: Float = 0f,
        val lowMid: Float = 0f,
        val mid: Float = 0f,
        val upMid: Float = 0f,
        val presence: Float = 0f,
        val brill: Float = 0f,
        val volume: Float = 0f
    )

    fun mapFft(fft: ByteArray): Bands {
        if (fft.isEmpty()) return Bands()
        val mags = FloatArray(fft.size / 2) { i ->
            val real = fft[i * 2].toFloat()
            val imag = fft[i * 2 + 1].toFloat()
            (real * real + imag * imag).pow(0.5f) / 255f
        }
        fun avg(minHz: Float, maxHz: Float): Float {
            val lo = max(1, (minHz / binHz).toInt())
            val hi = min(mags.size - 1, (maxHz / binHz).toInt())
            if (lo >= hi) return 0f
            var s = 0f
            for (i in lo..hi) s += mags[i]
            return (s / (hi - lo + 1)).coerceIn(0f, 1f)
        }
        return Bands(
            sub = avg(20f, 60f),
            bass = avg(60f, 250f),
            lowMid = avg(250f, 500f),
            mid = avg(500f, 2000f),
            upMid = avg(2000f, 4000f),
            presence = avg(4000f, 6000f),
            brill = avg(6000f, 20000f),
            volume = mags.average().toFloat()
        )
    }

    /** Linear interpolation from a rule. */
    fun evaluateRule(value: Float, inputMin: Float, inputMax: Float, outputMin: Float, outputMax: Float): Float {
        val t = ((value - inputMin) / (inputMax - inputMin)).coerceIn(0f, 1f)
        return outputMin + (outputMax - outputMin) * t
    }
}