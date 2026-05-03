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

audio_io_string_view ToStringView(const std::string& value) {
    return audio_io_string_view{value.data(), value.size()};
}

jstring ToJString(JNIEnv* env, const audio_io_owned_string& value) {
    if (value.data == nullptr || value.size == 0) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(value.data);
}
