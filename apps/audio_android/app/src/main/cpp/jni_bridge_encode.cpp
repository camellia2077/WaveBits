#include "jni_bridge_internal.h"

#include <array>

namespace jni_bridge {

jshortArray NativeEncodeTextToPcm(
    JNIEnv* env,
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
jint NativeValidateEncodeRequest(
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
    const bag_validation_issue issue = bag_validate_encode_request(&config, input.c_str());
    return static_cast<jint>(issue);
}
jobject NativeBuildEncodeFollowData(
    JNIEnv* env,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    const std::string input = JStringToStdString(env, text);
    if (input.empty()) {
        return NewEmptyEncodedAudioPayloadResult(env);
    }

    bag_encoder_config config =
        MakeEncoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);

    bag_encode_result result{};
    std::array<char, 4096> raw_bytes_hex_buffer{};
    std::array<char, 32768> raw_bits_binary_buffer{};
    std::vector<char> text_tokens_buffer(input.size() * 4 + 1, '\0');
    std::vector<char> lyric_lines_buffer(input.size() * 4 + 1, '\0');
    std::vector<bag_text_follow_token_entry> text_entries(input.size());
    std::vector<bag_text_follow_raw_segment_entry> text_raw_segments(input.size());
    std::vector<bag_text_follow_raw_display_unit_entry> text_raw_display_units(input.size() * 4);
    std::vector<bag_text_follow_lyric_line_entry> line_entries(input.size());
    std::vector<bag_text_follow_line_token_range_entry> line_token_ranges(input.size());
    std::vector<bag_text_follow_line_raw_segment_entry> line_raw_segments(input.size());
    std::vector<bag_payload_follow_byte_entry> byte_entries(input.size() * 4);
    std::vector<bag_payload_follow_binary_group_entry> binary_entries(input.size() * 8);

    result.raw_bytes_hex_buffer = raw_bytes_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_bytes_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_binary_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_binary_buffer.size();
    result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
    result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
    result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
    result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
    result.text_follow_data.text_token_timeline_buffer = text_entries.data();
    result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
    result.text_follow_data.token_raw_segments_buffer = text_raw_segments.data();
    result.text_follow_data.token_raw_segments_buffer_count = text_raw_segments.size();
    result.text_follow_data.token_raw_display_units_buffer = text_raw_display_units.data();
    result.text_follow_data.token_raw_display_units_buffer_count = text_raw_display_units.size();
    result.text_follow_data.lyric_line_timeline_buffer = line_entries.data();
    result.text_follow_data.lyric_line_timeline_buffer_count = line_entries.size();
    result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
    result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();
    result.text_follow_data.line_raw_segments_buffer = line_raw_segments.data();
    result.text_follow_data.line_raw_segments_buffer_count = line_raw_segments.size();
    result.follow_data.byte_timeline_buffer = byte_entries.data();
    result.follow_data.byte_timeline_buffer_count = byte_entries.size();
    result.follow_data.binary_group_timeline_buffer = binary_entries.data();
    result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();
    if (bag_build_encode_follow_data(&config, input.c_str(), &result) != BAG_OK) {
        return NewEmptyEncodedAudioPayloadResult(env);
    }

    return NewEncodedAudioPayloadResultFromEncodeResult(env, result);
}
jobject NativeDecodeGeneratedPcm(
    JNIEnv* env,
    jshortArray pcm,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    if (pcm == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_UNAVAILABLE), JNI_FALSE);
    }

    const jsize len = env->GetArrayLength(pcm);
    std::vector<int16_t> buffer(static_cast<size_t>(len), 0);
    env->GetShortArrayRegion(pcm, 0, len, reinterpret_cast<jshort*>(buffer.data()));

    bag_decoder_config config =
        MakeDecoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);
    bag_decoder* decoder = nullptr;
    if (bag_create_decoder(&config, &decoder) != BAG_OK || decoder == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    (void)bag_push_pcm(decoder, buffer.data(), buffer.size(), 0);
    bag_decode_result probe{};
    const bag_error_code probe_code = bag_poll_decode_result(decoder, &probe);
    bag_destroy_decoder(decoder);
    if (probe_code != BAG_OK) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    decoder = nullptr;
    if (bag_create_decoder(&config, &decoder) != BAG_OK || decoder == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }
    (void)bag_push_pcm(decoder, buffer.data(), buffer.size(), 0);

    std::vector<char> text_buffer(probe.text_size + 1, '\0');
    std::vector<char> raw_bytes_hex_buffer(probe.raw_bytes_hex_size + 1, '\0');
    std::vector<char> raw_bits_binary_buffer(probe.raw_bits_binary_size + 1, '\0');
    std::vector<char> text_tokens_buffer(probe.text_follow_data.text_tokens_size + 1, '\0');
    std::vector<char> lyric_lines_buffer(probe.text_follow_data.lyric_lines_size + 1, '\0');
    std::vector<bag_text_follow_token_entry> text_entries(
        probe.text_follow_data.text_token_timeline_count);
    std::vector<bag_text_follow_raw_segment_entry> text_raw_segments(
        probe.text_follow_data.token_raw_segments_count);
    std::vector<bag_text_follow_raw_display_unit_entry> text_raw_display_units(
        probe.text_follow_data.token_raw_display_units_count);
    std::vector<bag_text_follow_lyric_line_entry> line_entries(
        probe.text_follow_data.lyric_line_timeline_count);
    std::vector<bag_text_follow_line_token_range_entry> line_token_ranges(
        probe.text_follow_data.line_token_ranges_count);
    std::vector<bag_text_follow_line_raw_segment_entry> line_raw_segments(
        probe.text_follow_data.line_raw_segments_count);
    std::vector<bag_payload_follow_byte_entry> byte_entries(
        probe.follow_data.byte_timeline_count);
    std::vector<bag_payload_follow_binary_group_entry> binary_entries(
        probe.follow_data.binary_group_timeline_count);
    bag_decode_result result{};
    result.text_buffer = text_buffer.data();
    result.text_buffer_size = text_buffer.size();
    result.raw_bytes_hex_buffer = raw_bytes_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_bytes_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_binary_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_binary_buffer.size();
    result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
    result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
    result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
    result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
    result.text_follow_data.text_token_timeline_buffer = text_entries.data();
    result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
    result.text_follow_data.token_raw_segments_buffer = text_raw_segments.data();
    result.text_follow_data.token_raw_segments_buffer_count = text_raw_segments.size();
    result.text_follow_data.token_raw_display_units_buffer = text_raw_display_units.data();
    result.text_follow_data.token_raw_display_units_buffer_count = text_raw_display_units.size();
    result.text_follow_data.lyric_line_timeline_buffer = line_entries.data();
    result.text_follow_data.lyric_line_timeline_buffer_count = line_entries.size();
    result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
    result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();
    result.text_follow_data.line_raw_segments_buffer = line_raw_segments.data();
    result.text_follow_data.line_raw_segments_buffer_count = line_raw_segments.size();
    result.follow_data.byte_timeline_buffer = byte_entries.data();
    result.follow_data.byte_timeline_buffer_count = byte_entries.size();
    result.follow_data.binary_group_timeline_buffer = binary_entries.data();
    result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();
    if (bag_poll_decode_result(decoder, &result) != BAG_OK) {
        bag_destroy_decoder(decoder);
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }
    bag_destroy_decoder(decoder);
    text_entries.resize(result.text_follow_data.text_token_timeline_count);
    text_raw_segments.resize(result.text_follow_data.token_raw_segments_count);
    text_raw_display_units.resize(result.text_follow_data.token_raw_display_units_count);
    line_entries.resize(result.text_follow_data.lyric_line_timeline_count);
    line_token_ranges.resize(result.text_follow_data.line_token_ranges_count);
    line_raw_segments.resize(result.text_follow_data.line_raw_segments_count);
    byte_entries.resize(result.follow_data.byte_timeline_count);
    binary_entries.resize(result.follow_data.binary_group_timeline_count);
    const std::string text(text_buffer.data(), result.text_size);
    const std::string text_tokens(text_tokens_buffer.data(), result.text_follow_data.text_tokens_size);
    const std::string lyric_lines(lyric_lines_buffer.data(), result.text_follow_data.lyric_lines_size);
    const std::string raw_bytes_hex(raw_bytes_hex_buffer.data(), result.raw_bytes_hex_size);
    const std::string raw_bits_binary(raw_bits_binary_buffer.data(), result.raw_bits_binary_size);
    jobject decoded_payload = NewDecodedPayloadViewData(
        env,
        text,
        raw_bytes_hex,
        raw_bits_binary,
        static_cast<jint>(result.text_decode_status),
        result.raw_payload_available != 0 ? JNI_TRUE : JNI_FALSE);
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
    return NewDecodedAudioPayloadResult(env, decoded_payload, follow_data);
}
jint NativeValidateDecodeConfig(
    JNIEnv* env,
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
jstring NativeGetCoreVersion(JNIEnv* env) {
    const char* version = bag_core_version();
    if (version == nullptr) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(version);
}

}  // namespace jni_bridge
