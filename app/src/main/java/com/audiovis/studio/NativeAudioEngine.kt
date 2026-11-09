package com.audiovis.studio

/**
 * JNI boundary for performance-sensitive audio analysis.
 * Native FFT integration and PCM capture are subsequent milestones.
 */
class NativeAudioEngine {
    external fun version(): String
    external fun processPcm(pcm: ShortArray, output: IntArray)

    companion object {
        init { System.loadLibrary("audiovis_native") }
    }
}
