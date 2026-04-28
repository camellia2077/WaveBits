#include "jni_bridge_internal.h"

namespace jni_bridge {

jlong NativeStartEncodeTextJob(
    JNIEnv* env,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    const std::string input = JStringToStdString(env, text);
    bag_encoder_config config =
        MakeEncoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);

    bag_encode_job* job = nullptr;
    if (bag_start_encode_text_job(&config, input.c_str(), &job) != BAG_OK || job == nullptr) {
        return 0L;
    }

    return static_cast<jlong>(reinterpret_cast<intptr_t>(job));
}
jfloatArray NativePollEncodeTextJob(
    JNIEnv* env,
    jlong handle) {
    bag_encode_job* job = HandleToEncodeJob(handle);
    if (job == nullptr) {
        return NewEncodeJobProgressArray(
            env,
            BAG_ENCODE_JOB_FAILED,
            BAG_ENCODE_JOB_PHASE_PREPARING_INPUT,
            0.0f,
            BAG_INVALID_ARGUMENT);
    }

    bag_encode_job_progress progress{};
    if (bag_poll_encode_text_job(job, &progress) != BAG_OK) {
        return NewEncodeJobProgressArray(
            env,
            BAG_ENCODE_JOB_FAILED,
            BAG_ENCODE_JOB_PHASE_PREPARING_INPUT,
            0.0f,
            BAG_INVALID_ARGUMENT);
    }

    return NewEncodeJobProgressArray(
        env,
        progress.state,
        progress.phase,
        progress.progress_0_to_1,
        progress.terminal_code);
}
jobject NativeTakeEncodeTextJobResult(
    JNIEnv* env,
    jlong handle) {
    bag_encode_job* job = HandleToEncodeJob(handle);
    if (job == nullptr) {
        return NewEmptyEncodedAudioPayloadResult(env);
    }

    bag_pcm16_result result{};
    if (bag_take_encode_text_job_result(job, &result) != BAG_OK) {
        return NewEmptyEncodedAudioPayloadResult(env);
    }
    if (!IsPcmSampleCountWithinJvmLimit(result.sample_count)) {
        bag_free_pcm16_result(&result);
        return NewEmptyEncodedAudioPayloadResult(env, kBagErrorEncodedAudioTooLarge);
    }
    jshortArray pcm = NewShortArrayFromPcmResult(env, result);
    bag_free_pcm16_result(&result);
    if (pcm == nullptr) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return NewEmptyEncodedAudioPayloadResult(env, kBagErrorInternal);
    }
    jobject follow_data = NewEmptyPayloadFollowViewData(env);
    return NewEncodedAudioPayloadResult(env, pcm, "", "", follow_data, kBagErrorOk);
}
jint NativeCancelEncodeTextJob(
    jlong handle) {
    bag_encode_job* job = HandleToEncodeJob(handle);
    if (job == nullptr) {
        return static_cast<jint>(BAG_INVALID_ARGUMENT);
    }
    return static_cast<jint>(bag_cancel_encode_text_job(job));
}
void NativeDestroyEncodeTextJob(
    jlong handle) {
    bag_destroy_encode_text_job(HandleToEncodeJob(handle));
}

}  // namespace jni_bridge
