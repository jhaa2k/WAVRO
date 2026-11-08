package com.wavro.studio.visualization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumMapperTest {
    private val mapper = SpectrumMapper(sampleRate = 44100, fftSize = 1024)

    @Test fun `empty fft returns zero bands`() {
        val b = mapper.mapFft(ByteArray(0))
        assertEquals(0f, b.bass, 0.001f)
    }

    @Test fun `max amplitude yields high band values`() {
        val fft = ByteArray(1024) { 127 }
        val b = mapper.mapFft(fft)
        assertTrue(b.volume > 0.1f)
    }

    @Test fun `rule interpolation clamps output`() {
        assertEquals(100f, mapper.evaluateRule(-1f, 0f, 1f, 100f, 200f), 0.01f)
        assertEquals(200f, mapper.evaluateRule(2f, 0f, 1f, 100f, 200f), 0.01f)
        assertEquals(150f, mapper.evaluateRule(0.5f, 0f, 1f, 100f, 200f), 0.01f)
    }
}