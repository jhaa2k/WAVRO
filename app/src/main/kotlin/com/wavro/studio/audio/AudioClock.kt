package com.wavro.studio.audio

/** Audio-master clock. Renderer time is always derived from the decoded audio timeline. */
class AudioClock {
    private var anchorPositionUs = 0L
    private var anchorRealtimeNs = 0L
    private var lastResyncUs = 0L
    private var playing = false

    fun start(audioPositionUs: Long, realtimeNs: Long) {
        anchorPositionUs = audioPositionUs.coerceAtLeast(0L)
        anchorRealtimeNs = realtimeNs
        lastResyncUs = audioPositionUs
        playing = true
    }

    fun pause(audioPositionUs: Long, realtimeNs: Long) {
        anchorPositionUs = audioPositionUs.coerceAtLeast(0L)
        anchorRealtimeNs = realtimeNs
        playing = false
    }

    fun seek(audioPositionUs: Long, realtimeNs: Long) {
        anchorPositionUs = audioPositionUs.coerceAtLeast(0L)
        anchorRealtimeNs = realtimeNs
        lastResyncUs = audioPositionUs
    }

    fun positionUs(realtimeNs: Long): Long {
        if (!playing) return anchorPositionUs
        return (anchorPositionUs + ((realtimeNs - anchorRealtimeNs).coerceAtLeast(0L) / 1000L)).coerceAtLeast(0L)
    }

    /** Returns true when the renderer should reset analysis buffers after a seek or discontinuity. */
    fun synchronize(authoritativeAudioPositionUs: Long, realtimeNs: Long): Boolean {
        val predicted = positionUs(realtimeNs)
        val drift = authoritativeAudioPositionUs - predicted
        val needsReset = kotlin.math.abs(drift) > 20_000L
        if (needsReset || authoritativeAudioPositionUs - lastResyncUs >= 5_000_000L) {
            anchorPositionUs = authoritativeAudioPositionUs.coerceAtLeast(0L)
            anchorRealtimeNs = realtimeNs
            lastResyncUs = authoritativeAudioPositionUs
        }
        return needsReset
    }

    fun isPlaying(): Boolean = playing
}
