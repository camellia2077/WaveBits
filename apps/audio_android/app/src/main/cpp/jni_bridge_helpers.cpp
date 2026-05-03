#include "jni_bridge_internal.h"

#include <algorithm>
#include <array>

namespace jni_bridge {

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
                                     int flash_signal_profile,
                                     int flash_voicing_flavor) {
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
                                     int flash_signal_profile,
                                     int flash_voicing_flavor) {
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

std::vector<std::string> SplitOnSpaces(const std::string& value) {
    std::vector<std::string> tokens;
    std::size_t token_begin = 0;
    while (token_begin < value.size()) {
        while (token_begin < value.size() && value[token_begin] == ' ') {
            ++token_begin;
        }
        if (token_begin >= value.size()) {
            break;
        }
        const std::size_t token_end = value.find(' ', token_begin);
        if (token_end == std::string::npos) {
            tokens.push_back(value.substr(token_begin));
            break;
        }
        tokens.push_back(value.substr(token_begin, token_end - token_begin));
        token_begin = token_end + 1;
    }
    return tokens;
}

std::vector<std::string> SplitOnLines(const std::string& value) {
    std::vector<std::string> tokens;
    std::size_t token_begin = 0;
    while (token_begin <= value.size()) {
        const std::size_t token_end = value.find('\n', token_begin);
        if (token_end == std::string::npos) {
            if (token_begin < value.size()) {
                tokens.push_back(value.substr(token_begin));
            }
            break;
        }
        tokens.push_back(value.substr(token_begin, token_end - token_begin));
        token_begin = token_end + 1;
    }
    return tokens;
}

std::string RemoveSpaces(const std::string& value) {
    std::string compact;
    compact.reserve(value.size());
    for (const char ch : value) {
        if (ch != ' ') {
            compact.push_back(ch);
        }
    }
    return compact;
}

jclass FindClassOrNull(JNIEnv* env, const char* name) {
    return env->FindClass(name);
}

jobject NewArrayList(JNIEnv* env, jint initial_capacity) {
    jclass list_class = FindClassOrNull(env, "java/util/ArrayList");
    if (list_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(list_class, "<init>", "(I)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    return env->NewObject(list_class, ctor, initial_capacity);
}

bool AddToList(JNIEnv* env, jobject list, jobject item) {
    if (list == nullptr || item == nullptr) {
        return false;
    }
    jclass list_class = FindClassOrNull(env, "java/util/ArrayList");
    if (list_class == nullptr) {
        return false;
    }
    jmethodID add = env->GetMethodID(list_class, "add", "(Ljava/lang/Object;)Z");
    if (add == nullptr) {
        return false;
    }
    return env->CallBooleanMethod(list, add, item) == JNI_TRUE;
}

jobject NewStringList(JNIEnv* env, const std::vector<std::string>& values) {
    jobject list = NewArrayList(env, static_cast<jint>(values.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& value : values) {
        jstring item = env->NewStringUTF(value.c_str());
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewPayloadFollowByteEntry(JNIEnv* env, const bag_payload_follow_byte_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/PayloadFollowByteTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(III)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    return env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.byte_index));
}

jobject NewPayloadFollowBinaryGroupEntry(
    JNIEnv* env,
    const bag_payload_follow_binary_group_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/PayloadFollowBinaryGroupTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(IIIII)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    return env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.group_index),
        static_cast<jint>(entry.bit_offset),
        static_cast<jint>(entry.bit_count));
}

jobject NewTextFollowTimelineEntry(JNIEnv* env, const bag_text_follow_token_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(III)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    return env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.token_index));
}

jobject NewTextFollowRawSegmentViewData(JNIEnv* env,
                                        const bag_text_follow_raw_segment_entry& entry,
                                        const std::vector<std::string>& hex_tokens,
                                        const std::string& compact_bits) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowRawSegmentViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(IIIIILjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::string hex_text;
    for (std::size_t index = 0; index < entry.byte_count; ++index) {
        const std::size_t byte_index = entry.byte_offset + index;
        if (byte_index >= hex_tokens.size()) {
            break;
        }
        if (!hex_text.empty()) {
            hex_text.push_back(' ');
        }
        hex_text.append(hex_tokens[byte_index]);
    }

    std::string binary_text;
    const std::size_t bit_offset = entry.byte_offset * static_cast<std::size_t>(8);
    const std::size_t bit_count = entry.byte_count * static_cast<std::size_t>(8);
    if (bit_offset < compact_bits.size()) {
        const std::size_t clamped_bit_count =
            std::min(bit_count, compact_bits.size() - bit_offset);
        for (std::size_t index = 0; index < clamped_bit_count; ++index) {
            if (index > 0 && index % static_cast<std::size_t>(8) == 0) {
                binary_text.push_back(' ');
            }
            binary_text.push_back(compact_bits[bit_offset + index]);
        }
    }

    jstring hex_value = env->NewStringUTF(hex_text.c_str());
    jstring binary_value = env->NewStringUTF(binary_text.c_str());
    return env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.token_index),
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.byte_offset),
        static_cast<jint>(entry.byte_count),
        hex_value,
        binary_value);
}

jobject NewTextFollowRawDisplayUnitViewData(
    JNIEnv* env,
    const bag_text_follow_raw_display_unit_entry& entry,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowRawDisplayUnitViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(entry_class, "<init>", "(IIIIIILjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::string hex_text;
    for (std::size_t index = 0; index < entry.byte_count; ++index) {
        const std::size_t byte_index = entry.byte_offset + index;
        if (byte_index >= hex_tokens.size()) {
            break;
        }
        if (!hex_text.empty()) {
            hex_text.push_back(' ');
        }
        hex_text.append(hex_tokens[byte_index]);
    }

    std::string binary_text;
    const std::size_t bit_offset = entry.byte_offset * static_cast<std::size_t>(8);
    const std::size_t bit_count = entry.byte_count * static_cast<std::size_t>(8);
    if (bit_offset < compact_bits.size()) {
        const std::size_t clamped_bit_count =
            std::min(bit_count, compact_bits.size() - bit_offset);
        for (std::size_t index = 0; index < clamped_bit_count; ++index) {
            if (index > 0 && index % static_cast<std::size_t>(8) == 0) {
                binary_text.push_back(' ');
            }
            binary_text.push_back(compact_bits[bit_offset + index]);
        }
    }

    jstring hex_value = env->NewStringUTF(hex_text.c_str());
    jstring binary_value = env->NewStringUTF(binary_text.c_str());
    return env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.token_index),
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.byte_index_within_token),
        static_cast<jint>(entry.byte_offset),
        static_cast<jint>(entry.byte_count),
        hex_value,
        binary_value);
}

jobject NewTextFollowLyricLineTimelineEntry(JNIEnv* env,
                                            const bag_text_follow_lyric_line_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowLyricLineTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(III)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    return env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.line_index));
}

jobject NewTextFollowLineTokenRangeViewData(
    JNIEnv* env,
    const bag_text_follow_line_token_range_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowLineTokenRangeViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(III)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    return env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.line_index),
        static_cast<jint>(entry.token_begin_index),
        static_cast<jint>(entry.token_count));
}

jobject NewTextFollowLineRawSegmentViewData(JNIEnv* env,
                                            const bag_text_follow_line_raw_segment_entry& entry,
                                            const std::vector<std::string>& hex_tokens,
                                            const std::string& compact_bits) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowLineRawSegmentViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(entry_class, "<init>", "(IIIIILjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::string hex_text;
    for (std::size_t index = 0; index < entry.byte_count; ++index) {
        const std::size_t byte_index = entry.byte_offset + index;
        if (byte_index >= hex_tokens.size()) {
            break;
        }
        if (!hex_text.empty()) {
            hex_text.push_back(' ');
        }
        hex_text.append(hex_tokens[byte_index]);
    }

    std::string binary_text;
    const std::size_t bit_offset = entry.byte_offset * static_cast<std::size_t>(8);
    const std::size_t bit_count = entry.byte_count * static_cast<std::size_t>(8);
    if (bit_offset < compact_bits.size()) {
        const std::size_t clamped_bit_count =
            std::min(bit_count, compact_bits.size() - bit_offset);
        for (std::size_t index = 0; index < clamped_bit_count; ++index) {
            if (index > 0 && index % static_cast<std::size_t>(8) == 0) {
                binary_text.push_back(' ');
            }
            binary_text.push_back(compact_bits[bit_offset + index]);
        }
    }

    jstring hex_value = env->NewStringUTF(hex_text.c_str());
    jstring binary_value = env->NewStringUTF(binary_text.c_str());
    return env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.line_index),
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.byte_offset),
        static_cast<jint>(entry.byte_count),
        hex_value,
        binary_value);
}

jobject NewByteTimelineList(JNIEnv* env,
                            const std::vector<bag_payload_follow_byte_entry>& entries) {
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewPayloadFollowByteEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewBinaryTimelineList(
    JNIEnv* env,
    const std::vector<bag_payload_follow_binary_group_entry>& entries) {
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewPayloadFollowBinaryGroupEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewTextTimelineList(JNIEnv* env,
                            const std::vector<bag_text_follow_token_entry>& entries) {
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowTimelineEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewTextRawSegmentList(JNIEnv* env,
                              const std::vector<bag_text_follow_raw_segment_entry>& entries,
                              const std::vector<std::string>& hex_tokens,
                              const std::string& compact_bits) {
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowRawSegmentViewData(env, entry, hex_tokens, compact_bits);
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewTextRawDisplayUnitList(
    JNIEnv* env,
    const std::vector<bag_text_follow_raw_display_unit_entry>& entries,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits) {
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowRawDisplayUnitViewData(env, entry, hex_tokens, compact_bits);
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewLyricLineTimelineList(JNIEnv* env,
                                 const std::vector<bag_text_follow_lyric_line_entry>& entries) {
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowLyricLineTimelineEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewLineTokenRangeList(
    JNIEnv* env,
    const std::vector<bag_text_follow_line_token_range_entry>& entries) {
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowLineTokenRangeViewData(env, entry);
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewLineRawSegmentList(JNIEnv* env,
                              const std::vector<bag_text_follow_line_raw_segment_entry>& entries,
                              const std::vector<std::string>& hex_tokens,
                              const std::string& compact_bits) {
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()));
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowLineRawSegmentViewData(env, entry, hex_tokens, compact_bits);
        if (item == nullptr || !AddToList(env, list, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewPayloadFollowViewData(JNIEnv* env,
                                 const std::string& text_tokens,
                                 const std::string& lyric_lines,
                                 const std::string& raw_bytes_hex,
                                 const std::string& raw_bits_binary,
                                 const std::vector<bag_text_follow_token_entry>& text_entries,
                                 const std::vector<bag_text_follow_raw_segment_entry>& text_raw_segments,
                                 const std::vector<bag_text_follow_raw_display_unit_entry>& text_raw_display_units,
                                 const std::vector<bag_text_follow_lyric_line_entry>& line_entries,
                                 const std::vector<bag_text_follow_line_token_range_entry>& line_token_ranges,
                                 const std::vector<bag_text_follow_line_raw_segment_entry>& line_raw_segments,
                                 const std::vector<bag_payload_follow_byte_entry>& byte_entries,
                                 const std::vector<bag_payload_follow_binary_group_entry>& binary_entries,
                                 jboolean text_follow_available,
                                 jboolean lyric_line_follow_available,
                                 jint payload_begin_sample,
                                 jint payload_sample_count,
                                 jint total_pcm_sample_count,
                                 jboolean follow_available) {
    jclass result_class = FindClassOrNull(env, "com/bag/audioandroid/domain/PayloadFollowViewData");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            result_class,
            "<init>",
            "(Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;ZLjava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;ZLjava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;IIIZ)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::vector<std::string> text_follow_tokens = SplitOnLines(text_tokens);
    std::vector<std::string> lyric_line_tokens = SplitOnLines(lyric_lines);
    std::vector<std::string> hex_tokens = SplitOnSpaces(raw_bytes_hex);
    std::vector<std::string> binary_tokens;
    const std::string compact_bits = RemoveSpaces(raw_bits_binary);
    binary_tokens.reserve(binary_entries.size());
    for (const auto& entry : binary_entries) {
        const std::size_t bit_offset = entry.bit_offset;
        const std::size_t bit_count = entry.bit_count;
        if (bit_offset >= compact_bits.size()) {
            binary_tokens.emplace_back();
            continue;
        }
        const std::size_t token_size =
            std::min(bit_count, compact_bits.size() - bit_offset);
        binary_tokens.push_back(compact_bits.substr(bit_offset, token_size));
    }

    jobject text_list = NewStringList(env, text_follow_tokens);
    jobject text_timeline_list = NewTextTimelineList(env, text_entries);
    jobject text_raw_segment_list =
        NewTextRawSegmentList(env, text_raw_segments, hex_tokens, compact_bits);
    jobject text_raw_display_unit_list =
        NewTextRawDisplayUnitList(env, text_raw_display_units, hex_tokens, compact_bits);
    jobject lyric_line_list = NewStringList(env, lyric_line_tokens);
    jobject lyric_line_timeline_list = NewLyricLineTimelineList(env, line_entries);
    jobject line_token_range_list = NewLineTokenRangeList(env, line_token_ranges);
    jobject line_raw_segment_list =
        NewLineRawSegmentList(env, line_raw_segments, hex_tokens, compact_bits);
    jobject hex_list = NewStringList(env, hex_tokens);
    jobject binary_list = NewStringList(env, binary_tokens);
    jobject byte_timeline_list = NewByteTimelineList(env, byte_entries);
    jobject binary_timeline_list = NewBinaryTimelineList(env, binary_entries);
    if (text_list == nullptr || text_timeline_list == nullptr ||
        text_raw_segment_list == nullptr || text_raw_display_unit_list == nullptr ||
        lyric_line_list == nullptr ||
        lyric_line_timeline_list == nullptr || line_token_range_list == nullptr ||
        line_raw_segment_list == nullptr ||
        hex_list == nullptr || binary_list == nullptr ||
        byte_timeline_list == nullptr || binary_timeline_list == nullptr) {
        return nullptr;
    }
    return env->NewObject(
        result_class,
        ctor,
        text_list,
        text_timeline_list,
        text_raw_segment_list,
        text_raw_display_unit_list,
        text_follow_available,
        lyric_line_list,
        lyric_line_timeline_list,
        line_token_range_list,
        line_raw_segment_list,
        lyric_line_follow_available,
        hex_list,
        binary_list,
        byte_timeline_list,
        binary_timeline_list,
        payload_begin_sample,
        payload_sample_count,
        total_pcm_sample_count,
        follow_available);
}

jobject NewDecodedPayloadViewData(JNIEnv* env,
                                  const std::string& text,
                                  const std::string& raw_bytes_hex,
                                  const std::string& raw_bits_binary,
                                  jint text_decode_status_code,
                                  jboolean raw_payload_available) {
    jclass result_class = env->FindClass("com/bag/audioandroid/domain/DecodedPayloadViewData");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(result_class, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZ)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jstring text_value = env->NewStringUTF(text.c_str());
    jstring raw_bytes_hex_value = env->NewStringUTF(raw_bytes_hex.c_str());
    jstring raw_bits_binary_value = env->NewStringUTF(raw_bits_binary.c_str());
    return env->NewObject(
        result_class,
        ctor,
        text_value,
        raw_bytes_hex_value,
        raw_bits_binary_value,
        text_decode_status_code,
        raw_payload_available);
}

jobject NewDecodedAudioPayloadResult(JNIEnv* env,
                                     jobject decoded_payload,
                                     jobject follow_data) {
    jclass result_class = FindClassOrNull(env, "com/bag/audioandroid/domain/DecodedAudioPayloadResult");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            result_class,
            "<init>",
            "(Lcom/bag/audioandroid/domain/DecodedPayloadViewData;Lcom/bag/audioandroid/domain/PayloadFollowViewData;)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    return env->NewObject(result_class, ctor, decoded_payload, follow_data);
}

jobject NewEncodedAudioPayloadResult(JNIEnv* env,
                                     jshortArray pcm,
                                     const std::string& raw_bytes_hex,
                                     const std::string& raw_bits_binary,
                                     jobject follow_data,
                                     jint terminal_code) {
    jclass result_class = FindClassOrNull(env, "com/bag/audioandroid/domain/EncodedAudioPayloadResult");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            result_class,
            "<init>",
            "([SLjava/lang/String;Ljava/lang/String;Lcom/bag/audioandroid/domain/PayloadFollowViewData;I)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jstring raw_bytes_hex_value = env->NewStringUTF(raw_bytes_hex.c_str());
    jstring raw_bits_binary_value = env->NewStringUTF(raw_bits_binary.c_str());
    return env->NewObject(
        result_class,
        ctor,
        pcm,
        raw_bytes_hex_value,
        raw_bits_binary_value,
        follow_data,
        terminal_code);
}

jobject NewFlashSignalInfo(JNIEnv* env,
                           const std::string& low_carrier_hz,
                           const std::string& high_carrier_hz,
                           const std::string& bit_duration_samples,
                           const std::string& payload_silence,
                           const std::string& decode_path,
                           jboolean available) {
    jclass result_class = FindClassOrNull(env, "com/bag/audioandroid/domain/FlashSignalInfo");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            result_class,
            "<init>",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jstring low_value = env->NewStringUTF(low_carrier_hz.c_str());
    jstring high_value = env->NewStringUTF(high_carrier_hz.c_str());
    jstring bit_value = env->NewStringUTF(bit_duration_samples.c_str());
    jstring silence_value = env->NewStringUTF(payload_silence.c_str());
    jstring decode_value = env->NewStringUTF(decode_path.c_str());
    return env->NewObject(
        result_class,
        ctor,
        low_value,
        high_value,
        bit_value,
        silence_value,
        decode_value,
        available);
}

jshortArray NewShortArrayFromPcmResult(JNIEnv* env, const bag_pcm16_result& result) {
    jshortArray out = env->NewShortArray(static_cast<jsize>(result.sample_count));
    if (out != nullptr && result.sample_count > 0) {
        env->SetShortArrayRegion(
            out, 0, static_cast<jsize>(result.sample_count),
            reinterpret_cast<const jshort*>(result.samples));
    }
    return out;
}

jobject NewEmptyPayloadFollowViewData(JNIEnv* env) {
    return NewPayloadFollowViewData(
        env, "", "", "", "", {}, {}, {}, {}, {}, {}, {}, {}, JNI_FALSE, JNI_FALSE, 0, 0, 0, JNI_FALSE);
}

jobject NewEmptyDecodedAudioPayloadResult(JNIEnv* env,
                                          jint text_decode_status_code,
                                          jboolean raw_payload_available) {
    jobject decoded_payload = NewDecodedPayloadViewData(
        env, "", "", "", text_decode_status_code, raw_payload_available);
    jobject follow_data = NewEmptyPayloadFollowViewData(env);
    return NewDecodedAudioPayloadResult(env, decoded_payload, follow_data);
}

jobject NewEmptyEncodedAudioPayloadResult(JNIEnv* env, jint terminal_code) {
    jshortArray empty_pcm = env->NewShortArray(0);
    jobject follow_data = NewEmptyPayloadFollowViewData(env);
    return NewEncodedAudioPayloadResult(env, empty_pcm, "", "", follow_data, terminal_code);
}

std::string CopyApiString(const char* buffer, std::size_t size) {
    if (buffer == nullptr || size == 0) {
        return {};
    }
    return std::string(buffer, size);
}

jobject NewEncodedAudioPayloadResultFromEncodeResult(JNIEnv* env,
                                                     const bag_encode_result& result) {
    const std::string text_tokens = CopyApiString(
        result.text_follow_data.text_tokens_buffer,
        result.text_follow_data.text_tokens_size);
    const std::string lyric_lines = CopyApiString(
        result.text_follow_data.lyric_lines_buffer,
        result.text_follow_data.lyric_lines_size);
    const std::string raw_bytes_hex =
        CopyApiString(result.raw_bytes_hex_buffer, result.raw_bytes_hex_size);
    const std::string raw_bits_binary = CopyApiString(
        result.raw_bits_binary_buffer, result.raw_bits_binary_size);
    std::vector<bag_text_follow_token_entry> text_entries(
        result.text_follow_data.text_token_timeline_count);
    std::vector<bag_text_follow_raw_segment_entry> text_raw_segments(
        result.text_follow_data.token_raw_segments_count);
    std::vector<bag_text_follow_raw_display_unit_entry> text_raw_display_units(
        result.text_follow_data.token_raw_display_units_count);
    std::vector<bag_text_follow_lyric_line_entry> line_entries(
        result.text_follow_data.lyric_line_timeline_count);
    std::vector<bag_text_follow_line_token_range_entry> line_token_ranges(
        result.text_follow_data.line_token_ranges_count);
    std::vector<bag_text_follow_line_raw_segment_entry> line_raw_segments(
        result.text_follow_data.line_raw_segments_count);
    std::vector<bag_payload_follow_byte_entry> byte_entries(
        result.follow_data.byte_timeline_count);
    std::vector<bag_payload_follow_binary_group_entry> binary_entries(
        result.follow_data.binary_group_timeline_count);
    if (!text_entries.empty()) {
        std::copy_n(result.text_follow_data.text_token_timeline_buffer,
                    text_entries.size(), text_entries.begin());
    }
    if (!text_raw_segments.empty()) {
        std::copy_n(result.text_follow_data.token_raw_segments_buffer,
                    text_raw_segments.size(), text_raw_segments.begin());
    }
    if (!text_raw_display_units.empty()) {
        std::copy_n(result.text_follow_data.token_raw_display_units_buffer,
                    text_raw_display_units.size(),
                    text_raw_display_units.begin());
    }
    if (!line_entries.empty()) {
        std::copy_n(result.text_follow_data.lyric_line_timeline_buffer,
                    line_entries.size(), line_entries.begin());
    }
    if (!line_token_ranges.empty()) {
        std::copy_n(result.text_follow_data.line_token_ranges_buffer,
                    line_token_ranges.size(), line_token_ranges.begin());
    }
    if (!line_raw_segments.empty()) {
        std::copy_n(result.text_follow_data.line_raw_segments_buffer,
                    line_raw_segments.size(), line_raw_segments.begin());
    }
    if (!byte_entries.empty()) {
        std::copy_n(result.follow_data.byte_timeline_buffer, byte_entries.size(),
                    byte_entries.begin());
    }
    if (!binary_entries.empty()) {
        std::copy_n(result.follow_data.binary_group_timeline_buffer,
                    binary_entries.size(), binary_entries.begin());
    }
    bag_pcm16_result pcm_result{};
    pcm_result.samples = result.samples;
    pcm_result.sample_count = result.sample_count;
    jshortArray pcm = NewShortArrayFromPcmResult(env, pcm_result);
    jobject follow_data = NewPayloadFollowViewData(
        env,
        text_tokens,
        lyric_lines,
        raw_bytes_hex,
        raw_bits_binary,
        text_entries,
        text_raw_segments,
        text_raw_display_units,
        line_entries,
        line_token_ranges,
        line_raw_segments,
        byte_entries,
        binary_entries,
        result.text_follow_data.available != 0 ? JNI_TRUE : JNI_FALSE,
        (result.text_follow_data.available != 0 &&
         result.text_follow_data.lyric_line_timeline_count > 0 &&
         result.text_follow_data.line_token_ranges_count > 0)
            ? JNI_TRUE
            : JNI_FALSE,
        static_cast<jint>(result.follow_data.payload_begin_sample),
        static_cast<jint>(result.follow_data.payload_sample_count),
        static_cast<jint>(result.follow_data.total_pcm_sample_count),
        result.follow_data.available != 0 ? JNI_TRUE : JNI_FALSE);
    return NewEncodedAudioPayloadResult(
        env,
        pcm,
        raw_bytes_hex,
        raw_bits_binary,
        follow_data,
        kBagErrorOk);
}

bool IsPcmSampleCountWithinJvmLimit(std::size_t sample_count) {
    return sample_count <= kMaxJvmEncodePcmSamples;
}

}  // namespace jni_bridge
