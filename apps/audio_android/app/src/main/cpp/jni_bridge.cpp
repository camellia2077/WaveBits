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

bag_encoder_config MakeEncoderConfig(int sample_rate_hz, int frame_samples) {
    bag_encoder_config config{};
    config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
    config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
    config.enable_diagnostics = 0;
    config.mode = BAG_TRANSPORT_FLASH;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(int sample_rate_hz, int frame_samples) {
    bag_decoder_config config{};
    config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
    config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
    config.enable_diagnostics = 0;
    config.mode = BAG_TRANSPORT_FLASH;
    config.reserved = 0;
    return config;
}
}  // namespace

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeEncodeTextToPcm(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode) {
    const std::string input = JStringToStdString(env, text);
    if (input.empty()) {
        return env->NewShortArray(0);
    }

    bag_encoder_config config = MakeEncoderConfig(sample_rate_hz, frame_samples);
    config.mode = static_cast<bag_transport_mode>(mode);
    bag_pcm16_result pcm{};
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

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeValidateEncodeRequest(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode) {
    const std::string input = JStringToStdString(env, text);
    bag_encoder_config config = MakeEncoderConfig(sample_rate_hz, frame_samples);
    config.mode = static_cast<bag_transport_mode>(mode);
    const bag_validation_issue issue = bag_validate_encode_request(&config, input.c_str());
    return env->NewStringUTF(bag_validation_issue_message(issue));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDecodeGeneratedPcm(
    JNIEnv* env,
    jobject /*thiz*/,
    jshortArray pcm,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode) {
    if (pcm == nullptr) {
        return env->NewStringUTF("");
    }

    const jsize len = env->GetArrayLength(pcm);
    std::vector<int16_t> buffer(static_cast<size_t>(len), 0);
    env->GetShortArrayRegion(pcm, 0, len, reinterpret_cast<jshort*>(buffer.data()));

    bag_decoder_config config = MakeDecoderConfig(sample_rate_hz, frame_samples);
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

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeValidateDecodeConfig(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode) {
    bag_decoder_config config = MakeDecoderConfig(sample_rate_hz, frame_samples);
    config.mode = static_cast<bag_transport_mode>(mode);
    const bag_validation_issue issue = bag_validate_decoder_config(&config);
    return env->NewStringUTF(bag_validation_issue_message(issue));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeErrorCodeMessage(
    JNIEnv* env,
    jobject /*thiz*/,
    jint code) {
    return env->NewStringUTF(bag_error_code_message(static_cast<bag_error_code>(code)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeGetCoreVersion(
    JNIEnv* env, jobject /*thiz*/) {
    const char* version = bag_core_version();
    if (version == nullptr) {
        return env->NewStringUTF("unknown");
    }
    return env->NewStringUTF(version);
}
