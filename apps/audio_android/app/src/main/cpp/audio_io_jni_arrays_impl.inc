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

jshortArray VectorToShortArray(JNIEnv* env, const std::int16_t* pcm_samples, std::size_t sample_count) {
    jshortArray out = env->NewShortArray(static_cast<jsize>(sample_count));
    if (out != nullptr && sample_count > 0) {
        env->SetShortArrayRegion(
            out,
            0,
            static_cast<jsize>(sample_count),
            reinterpret_cast<const jshort*>(pcm_samples));
    }
    return out;
}

jobject NewIntegerList(JNIEnv* env, const std::uint32_t* values, std::size_t count) {
    jclass array_list_class = env->FindClass("java/util/ArrayList");
    jclass integer_class = env->FindClass("java/lang/Integer");
    if (array_list_class == nullptr || integer_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(array_list_class, "<init>", "(I)V");
    jmethodID add = env->GetMethodID(array_list_class, "add", "(Ljava/lang/Object;)Z");
    jmethodID value_of = env->GetStaticMethodID(integer_class, "valueOf", "(I)Ljava/lang/Integer;");
    if (ctor == nullptr || add == nullptr || value_of == nullptr) {
        return nullptr;
    }

    jobject list = env->NewObject(array_list_class, ctor, static_cast<jint>(count));
    if (list == nullptr) {
        return nullptr;
    }
    for (std::size_t index = 0; index < count; ++index) {
        jobject boxed = env->CallStaticObjectMethod(
            integer_class,
            value_of,
            static_cast<jint>(values[index]));
        env->CallBooleanMethod(list, add, boxed);
    }
    return list;
}

bool ReadIntegerList(
    JNIEnv* env,
    jobject list_object,
    std::vector<std::uint32_t>* out_values
) {
    if (out_values == nullptr) {
        return false;
    }
    out_values->clear();
    if (list_object == nullptr) {
        return true;
    }

    jclass list_class = env->FindClass("java/util/List");
    jclass integer_class = env->FindClass("java/lang/Integer");
    if (list_class == nullptr || integer_class == nullptr) {
        return false;
    }
    jmethodID size = env->GetMethodID(list_class, "size", "()I");
    jmethodID get = env->GetMethodID(list_class, "get", "(I)Ljava/lang/Object;");
    jmethodID int_value = env->GetMethodID(integer_class, "intValue", "()I");
    if (size == nullptr || get == nullptr || int_value == nullptr) {
        return false;
    }

    const jint count = env->CallIntMethod(list_object, size);
    out_values->reserve(static_cast<std::size_t>(std::max<jint>(0, count)));
    for (jint index = 0; index < count; ++index) {
        jobject boxed = env->CallObjectMethod(list_object, get, index);
        if (boxed == nullptr) {
            return false;
        }
        const jint value = env->CallIntMethod(boxed, int_value);
        if (value <= 0) {
            return false;
        }
        out_values->push_back(static_cast<std::uint32_t>(value));
    }
    return true;
}
