#include <jni.h>
#include <cstdint>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_audiovis_studio_NativeAudioEngine_version(JNIEnv* env, jobject) {
    return env->NewStringUTF("AUDIOVIS native engine 0.1");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_audiovis_studio_NativeAudioEngine_processPcm(
        JNIEnv*, jobject, jshortArray pcm, jintArray output) {
    // Placeholder JNI bridge. Replace with windowing + FFT + band aggregation.
    // No audio data is processed in this starter build.
    (void)pcm;
    (void)output;
}
