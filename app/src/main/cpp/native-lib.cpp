#include <jni.h>
#include <cmath>

extern "C" JNIEXPORT void JNICALL
Java_com_wavro_studio_audio_NativeAudioEngine_calculateFFT(
        JNIEnv* env, jobject, jbyteArray audioData, jfloatArray outputBands) {
    jsize length = env->GetArrayLength(audioData);
    jbyte* data = env->GetByteArrayElements(audioData, nullptr);
    jsize bandCount = env->GetArrayLength(outputBands);
    jfloat* bands = env->GetFloatArrayElements(outputBands, nullptr);

    int chunkSize = length / bandCount;
    for (int i = 0; i < bandCount; i++) {
        float sum = 0;
        for (int j = 0; j < chunkSize; j++) {
            int index = i * chunkSize + j;
            if (index < length) {
                float val = (float)(data[index] + 128) / 255.0f; 
                sum += val * val;
            }
        }
        bands[i] = sqrt(sum / chunkSize);
    }
    env->ReleaseByteArrayElements(audioData, data, JNI_ABORT);
    env->ReleaseFloatArrayElements(outputBands, bands, 0);
}