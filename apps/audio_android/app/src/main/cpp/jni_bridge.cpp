#include <jni.h>

#include <cstdint>
#include <string>
#include <vector>

#include "bag_api.h"

namespace {
constexpr int kDefaultSampleRateHz = 44100;
constexpr int kDefaultFrameSamples = 2205;

std::string JStringToStdString(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }
    const std::string out(chars);
    env->ReleaseStringUTFChars(value, chars);
    return out;
}

int NormalizeSampleRate(int sample_rate_hz) {
    return sample_rate_hz > 0 ? sample_rate_hz : kDefaultSampleRateHz;
}

int NormalizeFrameSamples(int sample_rate_hz, int frame_samples) {
    if (frame_samples > 0) {
        return frame_samples;
    }
    const int normalized_sample_rate = NormalizeSampleRate(sample_rate_hz);
    return normalized_sample_rate > 0 ? normalized_sample_rate / 20 : kDefaultFrameSamples;
}

bag_encoder_config MakeEncoderConfig(int sample_rate_hz,
                                     int frame_samples,
                                     int flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                                     int flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST) {
    bag_encoder_config config{};
    config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
    config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
    config.enable_diagnostics = 0;
    config.mode = BAG_TRANSPORT_FLASH;
    config.flash_signal_profile = static_cast<bag_flash_signal_profile>(flash_signal_profile);
    config.flash_voicing_flavor = static_cast<bag_flash_voicing_flavor>(flash_voicing_flavor);
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(int sample_rate_hz,
                                     int frame_samples,
                                     int flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                                     int flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST) {
    bag_decoder_config config{};
    config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
    config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
    config.enable_diagnostics = 0;
    config.mode = BAG_TRANSPORT_FLASH;
    config.flash_signal_profile = static_cast<bag_flash_signal_profile>(flash_signal_profile);
    config.flash_voicing_flavor = static_cast<bag_flash_voicing_flavor>(flash_voicing_flavor);
    config.reserved = 0;
    return config;
}

bag_encode_job* HandleToEncodeJob(jlong handle) {
    return reinterpret_cast<bag_encode_job*>(static_cast<intptr_t>(handle));
}

jfloatArray NewEncodeJobProgressArray(JNIEnv* env,
                                      bag_encode_job_state state,
                                      bag_encode_job_phase phase,
                                      float progress_0_to_1,
                                      bag_error_code terminal_code) {
    jfloatArray out = env->NewFloatArray(4);
    if (out == nullptr) {
        return nullptr;
    }

    const jfloat values[4] = {
        static_cast<jfloat>(state),
        static_cast<jfloat>(phase),
        progress_0_to_1,
        static_cast<jfloat>(terminal_code),
    };
    env->SetFloatArrayRegion(out, 0, 4, values);
    return out;
}
}  // namespace

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
    const std::string input = JStringToStdString(env, text);
    if (input.empty()) {
        return env->NewShortArray(0);
    }

    bag_encoder_config config =
        MakeEncoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);
    bag_pcm16_result pcm{};
    // Android keeps reusing the stable bag_api encode path. Any formal flash
    // voicing stays internal to core instead of widening JNI/Kotlin surfaces.
    if (bag_encode_text(&config, input.c_str(), &pcm) != BAG_OK) {
        return env->NewShortArray(0);
    }

    jshortArray out = env->NewShortArray(static_cast<jsize>(pcm.sample_count));
    if (out != nullptr && pcm.sample_count > 0) {
        env->SetShortArrayRegion(
            out, 0, static_cast<jsize>(pcm.sample_count), reinterpret_cast<const jshort*>(pcm.samples));
    }
    bag_free_pcm16_result(&pcm);
    if (out == nullptr) {
        return out;
    }
    return out;
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
    const std::string input = JStringToStdString(env, text);
    bag_encoder_config config =
        MakeEncoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);
    const bag_validation_issue issue = bag_validate_encode_request(&config, input.c_str());
    return static_cast<jint>(issue);
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

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativePollEncodeTextJob(
    JNIEnv* env,
    jobject /*thiz*/,
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

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeTakeEncodeTextJobResult(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    bag_encode_job* job = HandleToEncodeJob(handle);
    if (job == nullptr) {
        return env->NewShortArray(0);
    }

    bag_pcm16_result pcm{};
    if (bag_take_encode_text_job_result(job, &pcm) != BAG_OK) {
        return env->NewShortArray(0);
    }

    jshortArray out = env->NewShortArray(static_cast<jsize>(pcm.sample_count));
    if (out != nullptr && pcm.sample_count > 0) {
        env->SetShortArrayRegion(
            out, 0, static_cast<jsize>(pcm.sample_count), reinterpret_cast<const jshort*>(pcm.samples));
    }
    bag_free_pcm16_result(&pcm);
    return out;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeCancelEncodeTextJob(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    bag_encode_job* job = HandleToEncodeJob(handle);
    if (job == nullptr) {
        return static_cast<jint>(BAG_INVALID_ARGUMENT);
    }
    return static_cast<jint>(bag_cancel_encode_text_job(job));
}

extern "C" JNIEXPORT void JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDestroyEncodeTextJob(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    bag_destroy_encode_text_job(HandleToEncodeJob(handle));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDecodeGeneratedPcm(
    JNIEnv* env,
    jobject /*thiz*/,
    jshortArray pcm,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    if (pcm == nullptr) {
        return env->NewStringUTF("");
    }

    const jsize len = env->GetArrayLength(pcm);
    std::vector<int16_t> buffer(static_cast<size_t>(len), 0);
    env->GetShortArrayRegion(pcm, 0, len, reinterpret_cast<jshort*>(buffer.data()));

    bag_decoder_config config =
        MakeDecoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);
    bag_decoder* decoder = nullptr;
    if (bag_create_decoder(&config, &decoder) != BAG_OK || decoder == nullptr) {
        return env->NewStringUTF("");
    }

    (void)bag_push_pcm(decoder, buffer.data(), buffer.size(), 0);

    char text_buffer[4096] = {0};
    bag_text_result result{};
    result.buffer = text_buffer;
    result.buffer_size = sizeof(text_buffer);
    if (bag_poll_result(decoder, &result) != BAG_OK) {
        bag_destroy_decoder(decoder);
        return env->NewStringUTF("");
    }

    bag_destroy_decoder(decoder);
    return env->NewStringUTF(text_buffer);
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
    bag_decoder_config config =
        MakeDecoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);
    const bag_validation_issue issue = bag_validate_decoder_config(&config);
    return static_cast<jint>(issue);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeGetCoreVersion(
    JNIEnv* env, jobject /*thiz*/) {
    const char* version = bag_core_version();
    if (version == nullptr) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(version);
}
