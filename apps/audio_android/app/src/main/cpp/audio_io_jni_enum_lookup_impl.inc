jobject FindFlashStyleById(JNIEnv* env, jobjectArray flash_entries, const char* target_id) {
    if (flash_entries == nullptr || target_id == nullptr) {
        return nullptr;
    }
    jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
    if (flash_style_class == nullptr) {
        return nullptr;
    }
    jmethodID get_id = env->GetMethodID(
        flash_style_class,
        "getId",
        "()Ljava/lang/String;");
    if (get_id == nullptr) {
        return nullptr;
    }
    const jsize count = env->GetArrayLength(flash_entries);
    for (jsize index = 0; index < count; ++index) {
        jobject entry = env->GetObjectArrayElement(flash_entries, index);
        if (entry == nullptr) {
            continue;
        }
        jstring id = static_cast<jstring>(env->CallObjectMethod(entry, get_id));
        const std::string id_text = JStringToStdString(env, id);
        if (id_text == target_id) {
            return entry;
        }
    }
    return nullptr;
}

jobject FindTransportModeByWireName(JNIEnv* env, jobjectArray transport_entries, const char* target_wire_name) {
    if (transport_entries == nullptr || target_wire_name == nullptr) {
        return nullptr;
    }
    jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
    if (transport_mode_class == nullptr) {
        return nullptr;
    }
    jmethodID get_wire_name = env->GetMethodID(
        transport_mode_class,
        "getWireName",
        "()Ljava/lang/String;");
    if (get_wire_name == nullptr) {
        return nullptr;
    }
    const jsize count = env->GetArrayLength(transport_entries);
    for (jsize index = 0; index < count; ++index) {
        jobject entry = env->GetObjectArrayElement(transport_entries, index);
        if (entry == nullptr) {
            continue;
        }
        jstring wire_name = static_cast<jstring>(env->CallObjectMethod(entry, get_wire_name));
        const std::string wire_name_text = JStringToStdString(env, wire_name);
        if (wire_name_text == target_wire_name) {
            return entry;
        }
    }
    return nullptr;
}

audio_io_metadata_mode MapTransportMode(JNIEnv* env, jobject mode_object) {
    if (mode_object == nullptr) {
        return AUDIO_IO_METADATA_MODE_UNKNOWN;
    }

    jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
    jmethodID get_wire_name = env->GetMethodID(
        transport_mode_class,
        "getWireName",
        "()Ljava/lang/String;");
    if (get_wire_name == nullptr) {
        return AUDIO_IO_METADATA_MODE_UNKNOWN;
    }

    jstring wire_name = static_cast<jstring>(env->CallObjectMethod(mode_object, get_wire_name));
    const std::string wire_name_text = JStringToStdString(env, wire_name);
    if (wire_name_text == "flash") {
        return AUDIO_IO_METADATA_MODE_FLASH;
    }
    if (wire_name_text == "pro") {
        return AUDIO_IO_METADATA_MODE_PRO;
    }
    if (wire_name_text == "ultra") {
        return AUDIO_IO_METADATA_MODE_ULTRA;
    }
    if (wire_name_text == "mini") {
        return AUDIO_IO_METADATA_MODE_MINI;
    }
    return AUDIO_IO_METADATA_MODE_UNKNOWN;
}

audio_io_metadata_flash_voicing_style MapFlashVoicingStyle(JNIEnv* env, jobject flash_style_object) {
    if (flash_style_object == nullptr) {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
    }

    jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
    jmethodID get_id = env->GetMethodID(
        flash_style_class,
        "getId",
        "()Ljava/lang/String;");
    if (get_id == nullptr) {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
    }

    jstring style_id = static_cast<jstring>(env->CallObjectMethod(flash_style_object, get_id));
    const std::string style_id_text = JStringToStdString(env, style_id);
    if (style_id_text == "steady") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STEADY;
    }
    if (style_id_text == "litany") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY;
    }
    if (style_id_text == "hostile") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE;
    }
    if (style_id_text == "collapse") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE;
    }
    if (style_id_text == "zeal") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL;
    }
    if (style_id_text == "void") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID;
    }
    return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
}

audio_io_metadata_input_source_kind MapInputSourceKind(JNIEnv* env, jobject input_source_object) {
    if (input_source_object == nullptr) {
        return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN;
    }

    jclass input_source_kind_class = env->FindClass("com/bag/audioandroid/domain/GeneratedAudioInputSourceKind");
    jmethodID get_name = env->GetMethodID(
        input_source_kind_class,
        "name",
        "()Ljava/lang/String;");
    if (get_name == nullptr) {
        return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN;
    }

    jstring kind_name = static_cast<jstring>(env->CallObjectMethod(input_source_object, get_name));
    const std::string kind_name_text = JStringToStdString(env, kind_name);
    if (kind_name_text == "Manual") {
        return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL;
    }
    if (kind_name_text == "Sample") {
        return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_SAMPLE;
    }
    return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN;
}
