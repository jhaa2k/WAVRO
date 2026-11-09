package com.wavro.studio.audio

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentTrack: Track? = null,
    val error: String? = null
)