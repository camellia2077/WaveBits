#include <jni.h>

#include <cstring>
#include <cstdint>
#include <vector>

#include "android_audio_io/audio_io_package.h"

namespace {

std::vector<std::int16_t> ShortArrayToVector(JNIEnv* env, jshortArray pcm_array) {
    if (pcm_array == nullptr) {
        return {};
    }
    const auto length = env->GetArrayLength(pcm_array);
    std::vector<std::int16_t> pcm(static_cast<std::size_t>(length), 0);
    if (length > 0) {
        env->GetShortArrayRegion(
            pcm_array,
            0,
            length,
            reinterpret_cast<jshort*>(pcm.data()));
    }
    return pcm;
}

jobject ToGeneratedAudioMetadata(
    JNIEnv* env,
    jint version,
    jint mode,
    jboolean has_flash_voicing_style,
    jint flash_voicing_style,
    const char* created_at_iso_utc,
    jlong duration_ms,
    jint frame_samples,
    jint pcm_sample_count,
    const char* app_version,
    const char* core_version
) {
    jclass metadata_class = env->FindClass("com/bag/audioandroid/domain/GeneratedAudioMetadata");
    jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
    jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
    if (metadata_class == nullptr || transport_mode_class == nullptr || flash_style_class == nullptr) {
        return nullptr;
    }

    jmethodID transport_values = env->GetStaticMethodID(
        transport_mode_class,
        "values",
        "()[Lcom/bag/audioandroid/ui/model/TransportModeOption;");
    jmethodID flash_values = env->GetStaticMethodID(
        flash_style_class,
        "values",
        "()[Lcom/bag/audioandroid/ui/model/FlashVoicingStyleOption;");
    if (transport_values == nullptr || flash_values == nullptr) {
        return nullptr;
    }

    jobjectArray transport_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(transport_mode_class, transport_values));
    jobjectArray flash_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(flash_style_class, flash_values));
    if (transport_entries == nullptr || flash_entries == nullptr) {
        return nullptr;
    }

    jobject mode_object = nullptr;
    switch (mode) {
        case 1:
            mode_object = env->GetObjectArrayElement(transport_entries, 0);
            break;
        case 2:
            mode_object = env->GetObjectArrayElement(transport_entries, 1);
            break;
        case 3:
            mode_object = env->GetObjectArrayElement(transport_entries, 2);
            break;
        default:
            return nullptr;
    }

    jobject flash_style_object = nullptr;
    if (has_flash_voicing_style == JNI_TRUE) {
        switch (flash_voicing_style) {
            case 1:
                flash_style_object = env->GetObjectArrayElement(flash_entries, 0);
                break;
            case 2:
                flash_style_object = env->GetObjectArrayElement(flash_entries, 1);
                break;
            default:
                return nullptr;
        }
    }

    jmethodID ctor = env->GetMethodID(
        metadata_class,
        "<init>",
        "(ILcom/bag/audioandroid/ui/model/TransportModeOption;Lcom/bag/audioandroid/ui/model/FlashVoicingStyleOption;Ljava/lang/String;JIILjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    jstring created_at_string = env->NewStringUTF(created_at_iso_utc != nullptr ? created_at_iso_utc : "");
    jstring app_version_string = env->NewStringUTF(app_version != nullptr ? app_version : "");
    jstring core_version_string = env->NewStringUTF(core_version != nullptr ? core_version : "");

    return env->NewObject(
        metadata_class,
        ctor,
        version,
        mode_object,
        flash_style_object,
        created_at_string,
        duration_ms,
        frame_samples,
        pcm_sample_count,
        app_version_string,
        core_version_string);
}

jshortArray VectorToShortArray(JNIEnv* env, const std::vector<std::int16_t>& pcm_samples) {
    jshortArray out = env->NewShortArray(static_cast<jsize>(pcm_samples.size()));
    if (out != nullptr && !pcm_samples.empty()) {
        env->SetShortArrayRegion(
            out,
            0,
            static_cast<jsize>(pcm_samples.size()),
            reinterpret_cast<const jshort*>(pcm_samples.data()));
    }
    return out;
}

}  // namespace

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_bag_audioandroid_NativeAudioIoBridge_nativeEncodeMonoPcm16ToWavBytes(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sample_rate_hz,
    jshortArray pcm,
    jobject metadata_object
) {
    const auto pcm_samples = ShortArrayToVector(env, pcm);
    android_audio_io::EncodedWaveBitsMetadata metadata{};
    android_audio_io::EncodedWaveBitsMetadata* metadata_ptr = nullptr;
    if (metadata_object != nullptr) {
        jclass metadata_class = env->GetObjectClass(metadata_object);
        jfieldID version_field = env->GetFieldID(metadata_class, "version", "I");
        jfieldID mode_field = env->GetFieldID(
            metadata_class,
            "mode",
            "Lcom/bag/audioandroid/ui/model/TransportModeOption;");
        jfieldID flash_style_field = env->GetFieldID(
            metadata_class,
            "flashVoicingStyle",
            "Lcom/bag/audioandroid/ui/model/FlashVoicingStyleOption;");
        jfieldID created_at_field = env->GetFieldID(
            metadata_class,
            "createdAtIsoUtc",
            "Ljava/lang/String;");
        jfieldID duration_field = env->GetFieldID(
            metadata_class,
            "durationMs",
            "J");
        jfieldID frame_samples_field = env->GetFieldID(
            metadata_class,
            "frameSamples",
            "I");
        jfieldID pcm_sample_count_field = env->GetFieldID(
            metadata_class,
            "pcmSampleCount",
            "I");
        jfieldID app_version_field = env->GetFieldID(
            metadata_class,
            "appVersion",
            "Ljava/lang/String;");
        jfieldID core_version_field = env->GetFieldID(
            metadata_class,
            "coreVersion",
            "Ljava/lang/String;");
        if (version_field == nullptr ||
            mode_field == nullptr ||
            flash_style_field == nullptr ||
            created_at_field == nullptr ||
            duration_field == nullptr ||
            frame_samples_field == nullptr ||
            pcm_sample_count_field == nullptr ||
            app_version_field == nullptr ||
            core_version_field == nullptr) {
            return env->NewByteArray(0);
        }

        metadata.version = static_cast<std::uint8_t>(env->GetIntField(metadata_object, version_field));
        jobject mode_object = env->GetObjectField(metadata_object, mode_field);
        jobject flash_style_object = env->GetObjectField(metadata_object, flash_style_field);
        jstring created_at_string = static_cast<jstring>(env->GetObjectField(metadata_object, created_at_field));
        const jlong duration_ms = env->GetLongField(metadata_object, duration_field);
        const jint frame_samples = env->GetIntField(metadata_object, frame_samples_field);
        const jint pcm_sample_count = env->GetIntField(metadata_object, pcm_sample_count_field);
        jstring app_version_string = static_cast<jstring>(env->GetObjectField(metadata_object, app_version_field));
        jstring core_version_string = static_cast<jstring>(env->GetObjectField(metadata_object, core_version_field));

        jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
        jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
        jmethodID get_wire_name = env->GetMethodID(
            transport_mode_class,
            "getWireName",
            "()Ljava/lang/String;");
        jmethodID get_id = env->GetMethodID(
            flash_style_class,
            "getId",
            "()Ljava/lang/String;");
        if (get_wire_name == nullptr ||
            get_id == nullptr ||
            mode_object == nullptr) {
            return env->NewByteArray(0);
        }

        auto map_mode = [&](jobject mode_enum) -> std::uint8_t {
            jstring wire_name = static_cast<jstring>(env->CallObjectMethod(mode_enum, get_wire_name));
            const char* chars = env->GetStringUTFChars(wire_name, nullptr);
            std::uint8_t mapped = 0;
            if (std::strcmp(chars, "flash") == 0) {
                mapped = 1;
            } else if (std::strcmp(chars, "pro") == 0) {
                mapped = 2;
            } else if (std::strcmp(chars, "ultra") == 0) {
                mapped = 3;
            }
            env->ReleaseStringUTFChars(wire_name, chars);
            return mapped;
        };

        metadata.mode = map_mode(mode_object);
        if (created_at_string != nullptr) {
            const char* chars = env->GetStringUTFChars(created_at_string, nullptr);
            metadata.created_at_iso_utc = chars != nullptr ? chars : "";
            if (chars != nullptr) {
                env->ReleaseStringUTFChars(created_at_string, chars);
            }
        }
        metadata.duration_ms = duration_ms >= 0 ? static_cast<std::uint32_t>(duration_ms) : 0u;
        metadata.frame_samples = frame_samples >= 0 ? static_cast<std::uint32_t>(frame_samples) : 0u;
        metadata.pcm_sample_count = pcm_sample_count >= 0 ? static_cast<std::uint32_t>(pcm_sample_count) : 0u;
        if (app_version_string != nullptr) {
            const char* chars = env->GetStringUTFChars(app_version_string, nullptr);
            metadata.app_version = chars != nullptr ? chars : "";
            if (chars != nullptr) {
                env->ReleaseStringUTFChars(app_version_string, chars);
            }
        }
        if (core_version_string != nullptr) {
            const char* chars = env->GetStringUTFChars(core_version_string, nullptr);
            metadata.core_version = chars != nullptr ? chars : "";
            if (chars != nullptr) {
                env->ReleaseStringUTFChars(core_version_string, chars);
            }
        }
        if (flash_style_object != nullptr) {
            metadata.has_flash_voicing_style = true;
            jstring style_id = static_cast<jstring>(env->CallObjectMethod(flash_style_object, get_id));
            const char* chars = env->GetStringUTFChars(style_id, nullptr);
            if (std::strcmp(chars, "coded_burst") == 0) {
                metadata.flash_voicing_style = 1;
            } else if (std::strcmp(chars, "ritual_chant") == 0) {
                metadata.flash_voicing_style = 2;
            }
            env->ReleaseStringUTFChars(style_id, chars);
        }
        metadata_ptr = &metadata;
    }

    const auto wav_bytes =
        android_audio_io::EncodeMonoPcm16ToWavBytes(sample_rate_hz, pcm_samples, metadata_ptr);
    jbyteArray out = env->NewByteArray(static_cast<jsize>(wav_bytes.size()));
    if (out != nullptr && !wav_bytes.empty()) {
        env->SetByteArrayRegion(
            out,
            0,
            static_cast<jsize>(wav_bytes.size()),
            reinterpret_cast<const jbyte*>(wav_bytes.data()));
    }
    return out;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeAudioIoBridge_nativeDecodeMonoPcm16WavBytes(
    JNIEnv* env,
    jobject /*thiz*/,
    jbyteArray wav_bytes_array
) {
    std::vector<std::uint8_t> wav_bytes;
    if (wav_bytes_array != nullptr) {
        const auto length = env->GetArrayLength(wav_bytes_array);
        wav_bytes.resize(static_cast<std::size_t>(length), 0);
        if (length > 0) {
            env->GetByteArrayRegion(
                wav_bytes_array,
                0,
                length,
                reinterpret_cast<jbyte*>(wav_bytes.data()));
        }
    }

    const auto decoded = android_audio_io::DecodeMonoPcm16WavBytes(wav_bytes);
    jclass result_class = env->FindClass("com/bag/audioandroid/domain/DecodedAudioData");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(
        result_class,
        "<init>",
        "(III[SLcom/bag/audioandroid/domain/GeneratedAudioMetadata;)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jshortArray pcm_array = VectorToShortArray(env, decoded.pcm_samples);
    jobject metadata_object = nullptr;
    if (decoded.metadata_status == android_audio_io::WaveBitsMetadataStatus::kOk) {
        metadata_object = ToGeneratedAudioMetadata(
            env,
            static_cast<jint>(decoded.metadata.version),
            static_cast<jint>(decoded.metadata.mode),
            decoded.metadata.has_flash_voicing_style ? JNI_TRUE : JNI_FALSE,
            static_cast<jint>(decoded.metadata.flash_voicing_style),
            decoded.metadata.created_at_iso_utc.c_str(),
            static_cast<jlong>(decoded.metadata.duration_ms),
            static_cast<jint>(decoded.metadata.frame_samples),
            static_cast<jint>(decoded.metadata.pcm_sample_count),
            decoded.metadata.app_version.c_str(),
            decoded.metadata.core_version.c_str());
    }
    return env->NewObject(
        result_class,
        ctor,
        static_cast<jint>(decoded.status),
        static_cast<jint>(decoded.sample_rate_hz),
        static_cast<jint>(decoded.channels),
        pcm_array,
        metadata_object);
}
