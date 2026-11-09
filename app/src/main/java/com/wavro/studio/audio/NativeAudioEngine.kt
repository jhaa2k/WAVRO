package com.wavro.studio.audio

class NativeAudioEngine {
    init {
        System.loadLibrary("wavroaudio")
    }
    external fun calculateFFT(audioData: ByteArray, outputBands: FloatArray)
}