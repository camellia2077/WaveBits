#include "jni_bridge_internal.h"

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeEncodeTextToPcm(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeEncodeTextToPcm(
        env,
        text,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeValidateEncodeRequest(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeValidateEncodeRequest(
        env,
        text,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeStartEncodeTextJob(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeStartEncodeTextJob(
        env,
        text,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativePollEncodeTextJob(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativePollEncodeTextJob(env, handle);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeTakeEncodeTextJobResult(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeTakeEncodeTextJobResult(env, handle);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeBuildEncodeFollowData(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeBuildEncodeFollowData(
        env,
        text,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeCancelEncodeTextJob(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeCancelEncodeTextJob(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDestroyEncodeTextJob(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    jni_bridge::NativeDestroyEncodeTextJob(handle);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDecodeGeneratedPcm(
    JNIEnv* env,
    jobject /*thiz*/,
    jshortArray pcm,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeDecodeGeneratedPcm(
        env,
        pcm,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeValidateDecodeConfig(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeValidateDecodeConfig(
        env,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeGetCoreVersion(
    JNIEnv* env,
    jobject /*thiz*/) {
    return jni_bridge::NativeGetCoreVersion(env);
}
