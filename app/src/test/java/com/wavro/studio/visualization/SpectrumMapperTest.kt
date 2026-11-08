package com.wavro.studio.visualization

import com.wavro.studio.audio.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumMapperTest {
    @Test
    fun mapsSilentInputToZeroes() {
        assertEquals(List(8) { 0f }, SpectrumMapper.fromFft(ByteArray(64), outputBins = 8))
    }

    @Test
    fun mapsStrongLowFrequencyPeakToVisibleBins() {
        val fft = ByteArray(128)
        fft[2] = 127

        val spectrum = SpectrumMapper.fromFft(fft, outputBins = 8)

        assertEquals(8, spectrum.size)
        assertTrue(spectrum.take(2).any { it > 0.9f })
        assertTrue(spectrum.all { it in 0f..1f })
    }

    @Test
    fun handlesEmptyAndInvalidOutputSizes() {
        assertEquals(List(PlaybackState.SPECTRUM_BINS) { 0f }, SpectrumMapper.fromFft(ByteArray(0)))
        assertEquals(emptyList<Float>(), SpectrumMapper.fromFft(ByteArray(16), outputBins = 0))
    }
}
