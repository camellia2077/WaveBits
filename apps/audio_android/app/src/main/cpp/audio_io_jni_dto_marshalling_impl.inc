jobject ToGeneratedAudioMetadata(JNIEnv* env, const audio_io_metadata& metadata, jint fallback_sample_rate_hz) {
    jclass metadata_class = env->FindClass("com/bag/audioandroid/domain/GeneratedAudioMetadata");
    jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
    jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
    jclass input_source_kind_class = env->FindClass("com/bag/audioandroid/domain/GeneratedAudioInputSourceKind");
    if (metadata_class == nullptr || transport_mode_class == nullptr || flash_style_class == nullptr ||
        input_source_kind_class == nullptr) {
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
    jmethodID input_source_values = env->GetStaticMethodID(
        input_source_kind_class,
        "values",
        "()[Lcom/bag/audioandroid/domain/GeneratedAudioInputSourceKind;");
    if (transport_values == nullptr || flash_values == nullptr || input_source_values == nullptr) {
        return nullptr;
    }

    jobjectArray transport_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(transport_mode_class, transport_values));
    jobjectArray flash_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(flash_style_class, flash_values));
    jobjectArray input_source_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(input_source_kind_class, input_source_values));
    if (transport_entries == nullptr || flash_entries == nullptr || input_source_entries == nullptr) {
        return nullptr;
    }

    jobject mode_object = nullptr;
    switch (metadata.mode) {
        case AUDIO_IO_METADATA_MODE_MINI:
            mode_object = FindTransportModeByWireName(env, transport_entries, "mini");
            break;
        case AUDIO_IO_METADATA_MODE_FLASH:
            mode_object = FindTransportModeByWireName(env, transport_entries, "flash");
            break;
        case AUDIO_IO_METADATA_MODE_PRO:
            mode_object = FindTransportModeByWireName(env, transport_entries, "pro");
            break;
        case AUDIO_IO_METADATA_MODE_ULTRA:
            mode_object = FindTransportModeByWireName(env, transport_entries, "ultra");
            break;
        default:
            return nullptr;
    }
    if (mode_object == nullptr) {
        return nullptr;
    }

    jobject flash_style_object = nullptr;
    if (metadata.has_flash_voicing_style != 0u) {
        switch (metadata.flash_voicing_style) {
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STEADY:
                flash_style_object = FindFlashStyleById(env, flash_entries, "steady");
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY:
                flash_style_object = FindFlashStyleById(env, flash_entries, "litany");
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE:
                flash_style_object = FindFlashStyleById(env, flash_entries, "hostile");
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE:
                flash_style_object = FindFlashStyleById(env, flash_entries, "collapse");
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL:
                flash_style_object = FindFlashStyleById(env, flash_entries, "zeal");
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID:
                flash_style_object = FindFlashStyleById(env, flash_entries, "void");
                break;
            default:
                return nullptr;
        }
        if (flash_style_object == nullptr) {
            return nullptr;
        }
    }

    jobject input_source_object = nullptr;
    switch (metadata.input_source_kind) {
        case AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL:
            input_source_object = env->GetObjectArrayElement(input_source_entries, 0);
            break;
        case AUDIO_IO_METADATA_INPUT_SOURCE_KIND_SAMPLE:
            input_source_object = env->GetObjectArrayElement(input_source_entries, 1);
            break;
        default:
            input_source_object = env->GetObjectArrayElement(input_source_entries, 0);
            break;
    }

    jmethodID ctor = env->GetMethodID(
        metadata_class,
        "<init>",
        "(ILcom/bag/audioandroid/ui/model/TransportModeOption;Lcom/bag/audioandroid/ui/model/FlashVoicingStyleOption;Ljava/lang/String;JIIIILcom/bag/audioandroid/domain/GeneratedAudioInputSourceKind;ILjava/lang/String;Ljava/lang/String;Ljava/util/List;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    jstring created_at_string = ToJString(env, metadata.created_at_iso_utc);
    jstring app_version_string = ToJString(env, metadata.app_version);
    jstring core_version_string = ToJString(env, metadata.core_version);
    jobject segment_sample_counts =
        NewIntegerList(env, metadata.segment_sample_counts, metadata.segment_sample_count_count);

    return env->NewObject(
        metadata_class,
        ctor,
        static_cast<jint>(metadata.version),
        mode_object,
        flash_style_object,
        created_at_string,
        static_cast<jlong>(metadata.duration_ms),
        static_cast<jint>(metadata.sample_rate_hz != 0u ? metadata.sample_rate_hz : fallback_sample_rate_hz),
        static_cast<jint>(metadata.frame_samples),
        static_cast<jint>(metadata.pcm_sample_count),
        static_cast<jint>(metadata.payload_byte_count),
        input_source_object,
        static_cast<jint>(metadata.segment_count),
        app_version_string,
        core_version_string,
        segment_sample_counts);
}

jobject ToWavAudioInfo(JNIEnv* env, audio_io_wav_status status, const audio_io_wav_info& info) {
    jclass info_class = env->FindClass("com/bag/audioandroid/domain/WavAudioInfo");
    if (info_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(
        info_class,
        "<init>",
        "(IIIIJJJJ)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    return env->NewObject(
        info_class,
        ctor,
        static_cast<jint>(status),
        static_cast<jint>(info.sample_rate_hz),
        static_cast<jint>(info.channels),
        static_cast<jint>(info.bits_per_sample),
        static_cast<jlong>(info.pcm_sample_count),
        static_cast<jlong>(info.data_byte_count),
        static_cast<jlong>(info.file_byte_count),
        static_cast<jlong>(info.duration_ms));
}
