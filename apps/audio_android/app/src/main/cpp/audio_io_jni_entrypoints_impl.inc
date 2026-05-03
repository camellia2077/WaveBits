extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_bag_audioandroid_NativeAudioIoBridge_nativeEncodeMonoPcm16ToWavBytes(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sample_rate_hz,
    jshortArray pcm,
    jobject metadata_object
) {
    const auto pcm_samples = ShortArrayToVector(env, pcm);

    audio_io_byte_buffer wav_bytes{};
    audio_io_wav_status status = AUDIO_IO_WAV_INVALID_ARGUMENT;
    std::string created_at_iso_utc;
    std::string app_version;
    std::string core_version;
    std::vector<std::uint32_t> segment_sample_counts;
    audio_io_metadata_view metadata{};

    if (metadata_object == nullptr) {
        status = audio_io_encode_mono_pcm16_wav(
            sample_rate_hz,
            pcm_samples.data(),
            pcm_samples.size(),
            &wav_bytes);
    } else {
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
        jfieldID sample_rate_hz_field = env->GetFieldID(
            metadata_class,
            "sampleRateHz",
            "I");
        jfieldID frame_samples_field = env->GetFieldID(
            metadata_class,
            "frameSamples",
            "I");
        jfieldID pcm_sample_count_field = env->GetFieldID(
            metadata_class,
            "pcmSampleCount",
            "I");
        jfieldID payload_byte_count_field = env->GetFieldID(
            metadata_class,
            "payloadByteCount",
            "I");
        jfieldID input_source_kind_field = env->GetFieldID(
            metadata_class,
            "inputSourceKind",
            "Lcom/bag/audioandroid/domain/GeneratedAudioInputSourceKind;");
        jfieldID segment_count_field = env->GetFieldID(
            metadata_class,
            "segmentCount",
            "I");
        jfieldID app_version_field = env->GetFieldID(
            metadata_class,
            "appVersion",
            "Ljava/lang/String;");
        jfieldID core_version_field = env->GetFieldID(
            metadata_class,
            "coreVersion",
            "Ljava/lang/String;");
        jfieldID segment_sample_counts_field = env->GetFieldID(
            metadata_class,
            "segmentSampleCounts",
            "Ljava/util/List;");
        if (version_field == nullptr ||
            mode_field == nullptr ||
            flash_style_field == nullptr ||
            created_at_field == nullptr ||
            duration_field == nullptr ||
            sample_rate_hz_field == nullptr ||
            frame_samples_field == nullptr ||
            pcm_sample_count_field == nullptr ||
            payload_byte_count_field == nullptr ||
            input_source_kind_field == nullptr ||
            segment_count_field == nullptr ||
            app_version_field == nullptr ||
            core_version_field == nullptr ||
            segment_sample_counts_field == nullptr) {
            return env->NewByteArray(0);
        }

        jobject mode_object = env->GetObjectField(metadata_object, mode_field);
        jobject flash_style_object = env->GetObjectField(metadata_object, flash_style_field);
        jobject input_source_kind_object = env->GetObjectField(metadata_object, input_source_kind_field);
        created_at_iso_utc = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, created_at_field)));
        app_version = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, app_version_field)));
        core_version = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, core_version_field)));
        if (!ReadIntegerList(
                env,
                env->GetObjectField(metadata_object, segment_sample_counts_field),
                &segment_sample_counts)) {
            return env->NewByteArray(0);
        }

        metadata.version = static_cast<std::uint8_t>(env->GetIntField(metadata_object, version_field));
        metadata.mode = MapTransportMode(env, mode_object);
        metadata.has_flash_voicing_style = flash_style_object != nullptr ? 1u : 0u;
        metadata.flash_voicing_style = MapFlashVoicingStyle(env, flash_style_object);
        metadata.created_at_iso_utc = ToStringView(created_at_iso_utc);
        metadata.duration_ms =
            static_cast<std::uint32_t>(std::max<jlong>(0, env->GetLongField(metadata_object, duration_field)));
        metadata.sample_rate_hz =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, sample_rate_hz_field)));
        metadata.frame_samples =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, frame_samples_field)));
        metadata.pcm_sample_count =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, pcm_sample_count_field)));
        metadata.payload_byte_count =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, payload_byte_count_field)));
        metadata.input_source_kind = MapInputSourceKind(env, input_source_kind_object);
        metadata.segment_count =
            static_cast<std::uint32_t>(std::max<jint>(1, env->GetIntField(metadata_object, segment_count_field)));
        metadata.segment_sample_counts = segment_sample_counts.data();
        metadata.segment_sample_count_count = segment_sample_counts.size();
        metadata.app_version = ToStringView(app_version);
        metadata.core_version = ToStringView(core_version);

        status = audio_io_encode_mono_pcm16_wav_with_metadata(
            sample_rate_hz,
            pcm_samples.data(),
            pcm_samples.size(),
            &metadata,
            &wav_bytes);
    }

    if (status != AUDIO_IO_WAV_OK) {
        audio_io_free_byte_buffer(&wav_bytes);
        return env->NewByteArray(0);
    }

    jbyteArray out = env->NewByteArray(static_cast<jsize>(wav_bytes.size));
    if (out != nullptr && wav_bytes.size > 0) {
        env->SetByteArrayRegion(
            out,
            0,
            static_cast<jsize>(wav_bytes.size),
            reinterpret_cast<const jbyte*>(wav_bytes.data));
    }
    audio_io_free_byte_buffer(&wav_bytes);
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

    audio_io_decoded_wav decoded{};
    const auto wav_status = audio_io_decode_mono_pcm16_wav(
        wav_bytes.data(),
        wav_bytes.size(),
        &decoded);

    jclass result_class = env->FindClass("com/bag/audioandroid/domain/DecodedAudioData");
    if (result_class == nullptr) {
        audio_io_free_decoded_wav(&decoded);
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(
        result_class,
        "<init>",
        "(IIII[SLcom/bag/audioandroid/domain/GeneratedAudioMetadata;)V");
    if (ctor == nullptr) {
        audio_io_free_decoded_wav(&decoded);
        return nullptr;
    }

    jshortArray pcm_array = VectorToShortArray(env, decoded.samples, decoded.sample_count);
    jobject metadata_object = nullptr;
    if (decoded.metadata_status == AUDIO_IO_METADATA_OK) {
        metadata_object = ToGeneratedAudioMetadata(env, decoded.metadata, static_cast<jint>(decoded.sample_rate_hz));
    }
    jobject result = env->NewObject(
        result_class,
        ctor,
        static_cast<jint>(wav_status),
        static_cast<jint>(decoded.metadata_status),
        static_cast<jint>(decoded.sample_rate_hz),
        static_cast<jint>(decoded.channels),
        pcm_array,
        metadata_object);
    audio_io_free_decoded_wav(&decoded);
    return result;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeAudioIoBridge_nativeProbeMonoPcm16WavBytes(
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

    audio_io_wav_info info{};
    const auto status = audio_io_probe_mono_pcm16_wav(
        wav_bytes.data(),
        wav_bytes.size(),
        &info);
    return ToWavAudioInfo(env, status, info);
}
