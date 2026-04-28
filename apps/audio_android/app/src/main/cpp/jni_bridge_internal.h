#pragma once

#include <jni.h>

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "bag_api.h"

namespace jni_bridge {

inline constexpr int kDefaultSampleRateHz = 44100;
inline constexpr int kDefaultFrameSamples = 2205;
inline constexpr jint kBagErrorOk = 0;
inline constexpr jint kBagErrorInternal = 4;
inline constexpr jint kBagErrorEncodedAudioTooLarge = 6;
inline constexpr std::size_t kMaxJvmEncodePcmSamples =
    static_cast<std::size_t>(kDefaultSampleRateHz) * 60U * 10U;

std::string JStringToStdString(JNIEnv* env, jstring value);

int NormalizeSampleRate(int sample_rate_hz);
int NormalizeFrameSamples(int sample_rate_hz, int frame_samples);

bag_encoder_config MakeEncoderConfig(int sample_rate_hz,
                                     int frame_samples,
                                     int flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                                     int flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST);

bag_decoder_config MakeDecoderConfig(int sample_rate_hz,
                                     int frame_samples,
                                     int flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                                     int flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST);

bag_encode_job* HandleToEncodeJob(jlong handle);

jfloatArray NewEncodeJobProgressArray(JNIEnv* env,
                                      bag_encode_job_state state,
                                      bag_encode_job_phase phase,
                                      float progress_0_to_1,
                                      bag_error_code terminal_code);

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
                                 jboolean follow_available);

jobject NewDecodedPayloadViewData(JNIEnv* env,
                                  const std::string& text,
                                  const std::string& raw_bytes_hex,
                                  const std::string& raw_bits_binary,
                                  jint text_decode_status_code,
                                  jboolean raw_payload_available);

jobject NewDecodedAudioPayloadResult(JNIEnv* env,
                                     jobject decoded_payload,
                                     jobject follow_data);

jobject NewEncodedAudioPayloadResult(JNIEnv* env,
                                     jshortArray pcm,
                                     const std::string& raw_bytes_hex,
                                     const std::string& raw_bits_binary,
                                     jobject follow_data,
                                     jint terminal_code);

jshortArray NewShortArrayFromPcmResult(JNIEnv* env, const bag_pcm16_result& result);
jobject NewEmptyPayloadFollowViewData(JNIEnv* env);
jobject NewEmptyDecodedAudioPayloadResult(JNIEnv* env,
                                          jint text_decode_status_code,
                                          jboolean raw_payload_available);
jobject NewEmptyEncodedAudioPayloadResult(JNIEnv* env,
                                          jint terminal_code = kBagErrorInternal);
jobject NewEncodedAudioPayloadResultFromEncodeResult(JNIEnv* env,
                                                     const bag_encode_result& result);
bool IsPcmSampleCountWithinJvmLimit(std::size_t sample_count);

jshortArray NativeEncodeTextToPcm(JNIEnv* env,
                                  jstring text,
                                  jint sample_rate_hz,
                                  jint frame_samples,
                                  jint mode,
                                  jint flash_signal_profile,
                                  jint flash_voicing_flavor);

jint NativeValidateEncodeRequest(JNIEnv* env,
                                 jstring text,
                                 jint sample_rate_hz,
                                 jint frame_samples,
                                 jint mode,
                                 jint flash_signal_profile,
                                 jint flash_voicing_flavor);

jobject NativeBuildEncodeFollowData(JNIEnv* env,
                                    jstring text,
                                    jint sample_rate_hz,
                                    jint frame_samples,
                                    jint mode,
                                    jint flash_signal_profile,
                                    jint flash_voicing_flavor);

jobject NativeDecodeGeneratedPcm(JNIEnv* env,
                                 jshortArray pcm,
                                 jint sample_rate_hz,
                                 jint frame_samples,
                                 jint mode,
                                 jint flash_signal_profile,
                                 jint flash_voicing_flavor);

jint NativeValidateDecodeConfig(JNIEnv* env,
                                jint sample_rate_hz,
                                jint frame_samples,
                                jint mode,
                                jint flash_signal_profile,
                                jint flash_voicing_flavor);

jstring NativeGetCoreVersion(JNIEnv* env);

jlong NativeStartEncodeTextJob(JNIEnv* env,
                               jstring text,
                               jint sample_rate_hz,
                               jint frame_samples,
                               jint mode,
                               jint flash_signal_profile,
                               jint flash_voicing_flavor);

jfloatArray NativePollEncodeTextJob(JNIEnv* env, jlong handle);
jobject NativeTakeEncodeTextJobResult(JNIEnv* env, jlong handle);
jint NativeCancelEncodeTextJob(jlong handle);
void NativeDestroyEncodeTextJob(jlong handle);

}  // namespace jni_bridge
