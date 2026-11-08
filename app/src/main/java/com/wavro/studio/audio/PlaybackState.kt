package com.wavro.studio.audio

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentTrackName: String? = null,
    val error: String? = null
)