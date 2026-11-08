package com.wavro.studio.audio

data class PlaybackState(
    val title: String = "No track selected",
    val artist: String = "Open an audio file or start the demo",
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isDemo: Boolean = false,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val spectrum: List<Float> = List(SPECTRUM_BINS) { 0f },
    val visualizationAvailable: Boolean = false,
    val error: String? = null,
) {
    companion object {
        const val SPECTRUM_BINS = 48
    }
}
