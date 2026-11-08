package com.wavro.studio.visualization

import com.wavro.studio.audio.PlaybackState

import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

object SpectrumMapper {
    fun fromFft(fft: ByteArray, outputBins: Int = PlaybackState.SPECTRUM_BINS): List<Float> {
        val sourceBins = fft.size / 2
        if (sourceBins < 2 || outputBins < 1) return List(outputBins.coerceAtLeast(0)) { 0f }

        return List(outputBins) { outputIndex ->
            val start = floor(2.0.pow(outputIndex.toDouble() / outputBins * log2(sourceBins.toDouble())))
                .toInt().coerceIn(1, sourceBins - 1)
            val end = floor(2.0.pow((outputIndex + 1.0) / outputBins * log2(sourceBins.toDouble())))
                .toInt().coerceIn(start + 1, sourceBins)
            var peak = 0.0
            for (bin in start until end) {
                val real = fft[bin * 2].toInt().toDouble()
                val imaginary = fft[bin * 2 + 1].toInt().toDouble()
                peak = maxOf(peak, hypot(real, imaginary))
            }
            val decibels = (20.0 * log10((peak / 128.0).coerceAtLeast(1e-4))).coerceIn(-72.0, 0.0)
            ((decibels + 72.0) / 72.0).toFloat()
        }
    }

    private fun log2(value: Double) = kotlin.math.ln(value) / kotlin.math.ln(2.0)
}
