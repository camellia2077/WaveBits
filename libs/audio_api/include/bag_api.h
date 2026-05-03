#pragma once

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum bag_error_code {
  BAG_OK = 0,
  BAG_INVALID_ARGUMENT = 1,
  BAG_NOT_READY = 2,
  BAG_NOT_IMPLEMENTED = 3,
  BAG_INTERNAL = 4,
  BAG_CANCELLED = 5
} bag_error_code;

typedef enum bag_transport_mode {
  BAG_TRANSPORT_MINI = 0,
  BAG_TRANSPORT_FLASH = 1,
  BAG_TRANSPORT_PRO = 2,
  BAG_TRANSPORT_ULTRA = 3
} bag_transport_mode;

typedef enum bag_flash_signal_profile {
  BAG_FLASH_SIGNAL_PROFILE_STEADY = 0,
  BAG_FLASH_SIGNAL_PROFILE_LITANY = 1,
  BAG_FLASH_SIGNAL_PROFILE_HOSTILE = 3,
  BAG_FLASH_SIGNAL_PROFILE_COLLAPSE = 4,
  BAG_FLASH_SIGNAL_PROFILE_ZEAL = 5,
  BAG_FLASH_SIGNAL_PROFILE_VOID = 6
} bag_flash_signal_profile;

typedef enum bag_flash_voicing_flavor {
  BAG_FLASH_VOICING_FLAVOR_STEADY = 0,
  BAG_FLASH_VOICING_FLAVOR_LITANY = 1,
  BAG_FLASH_VOICING_FLAVOR_HOSTILE = 3,
  BAG_FLASH_VOICING_FLAVOR_COLLAPSE = 4,
  BAG_FLASH_VOICING_FLAVOR_ZEAL = 5,
  BAG_FLASH_VOICING_FLAVOR_VOID = 6
} bag_flash_voicing_flavor;

typedef enum bag_validation_issue {
  BAG_VALIDATION_OK = 0,
  BAG_VALIDATION_NULL_CONFIG = 1,
  BAG_VALIDATION_NULL_TEXT = 2,
  BAG_VALIDATION_NULL_DECODER_OUTPUT = 3,
  BAG_VALIDATION_INVALID_SAMPLE_RATE = 4,
  BAG_VALIDATION_INVALID_FRAME_SAMPLES = 5,
  BAG_VALIDATION_INVALID_MODE = 6,
  BAG_VALIDATION_PRO_ASCII_ONLY = 7,
  BAG_VALIDATION_PAYLOAD_TOO_LARGE = 8,
  BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE = 9,
  BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR = 10,
  BAG_VALIDATION_MINI_MORSE_ONLY = 11
} bag_validation_issue;

typedef struct bag_decoder bag_decoder;
typedef struct bag_encode_job bag_encode_job;

typedef struct bag_encoder_config {
  int sample_rate_hz;
  int frame_samples;
  int enable_diagnostics;
  bag_transport_mode mode;
  bag_flash_signal_profile flash_signal_profile;
  bag_flash_voicing_flavor flash_voicing_flavor;
  int reserved;
} bag_encoder_config;

typedef struct bag_decoder_config {
  int sample_rate_hz;
  int frame_samples;
  int enable_diagnostics;
  bag_transport_mode mode;
  bag_flash_signal_profile flash_signal_profile;
  bag_flash_voicing_flavor flash_voicing_flavor;
  int reserved;
} bag_decoder_config;

typedef struct bag_text_result {
  char* buffer;
  size_t buffer_size;
  size_t text_size;
  int complete;
  float confidence;
  bag_transport_mode mode;
} bag_text_result;

typedef enum bag_decode_content_status {
  BAG_DECODE_CONTENT_STATUS_OK = 0,
  BAG_DECODE_CONTENT_STATUS_UNAVAILABLE = 1,
  BAG_DECODE_CONTENT_STATUS_INVALID_TEXT_PAYLOAD = 2,
  BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL = 3,
  BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR = 4
} bag_decode_content_status;

typedef struct bag_payload_follow_byte_entry {
  size_t start_sample;
  size_t sample_count;
  size_t byte_index;
} bag_payload_follow_byte_entry;

typedef struct bag_payload_follow_binary_group_entry {
  size_t start_sample;
  size_t sample_count;
  size_t group_index;
  size_t bit_offset;
  size_t bit_count;
} bag_payload_follow_binary_group_entry;

typedef struct bag_payload_follow_data {
  bag_payload_follow_byte_entry* byte_timeline_buffer;
  size_t byte_timeline_buffer_count;
  size_t byte_timeline_count;
  bag_decode_content_status byte_timeline_status;
  bag_payload_follow_binary_group_entry* binary_group_timeline_buffer;
  size_t binary_group_timeline_buffer_count;
  size_t binary_group_timeline_count;
  bag_decode_content_status binary_group_timeline_status;
  size_t payload_begin_sample;
  size_t payload_sample_count;
  size_t total_pcm_sample_count;
  int available;
} bag_payload_follow_data;

typedef struct bag_text_follow_token_entry {
  size_t start_sample;
  size_t sample_count;
  size_t token_index;
  size_t text_offset;
  size_t text_size;
} bag_text_follow_token_entry;

typedef struct bag_text_follow_raw_segment_entry {
  size_t start_sample;
  size_t sample_count;
  size_t token_index;
  size_t byte_offset;
  size_t byte_count;
} bag_text_follow_raw_segment_entry;

typedef struct bag_text_follow_raw_display_unit_entry {
  size_t start_sample;
  size_t sample_count;
  size_t token_index;
  size_t byte_index_within_token;
  size_t byte_offset;
  size_t byte_count;
} bag_text_follow_raw_display_unit_entry;

typedef struct bag_text_follow_lyric_line_entry {
  size_t start_sample;
  size_t sample_count;
  size_t line_index;
} bag_text_follow_lyric_line_entry;

typedef struct bag_text_follow_line_token_range_entry {
  size_t line_index;
  size_t token_begin_index;
  size_t token_count;
} bag_text_follow_line_token_range_entry;

typedef struct bag_text_follow_line_raw_segment_entry {
  size_t start_sample;
  size_t sample_count;
  size_t line_index;
  size_t byte_offset;
  size_t byte_count;
} bag_text_follow_line_raw_segment_entry;

typedef struct bag_text_follow_data {
  char* text_tokens_buffer;
  size_t text_tokens_buffer_size;
  size_t text_tokens_size;
  bag_decode_content_status text_tokens_status;
  bag_text_follow_token_entry* text_token_timeline_buffer;
  size_t text_token_timeline_buffer_count;
  size_t text_token_timeline_count;
  bag_decode_content_status text_token_timeline_status;
  bag_text_follow_raw_segment_entry* token_raw_segments_buffer;
  size_t token_raw_segments_buffer_count;
  size_t token_raw_segments_count;
  bag_decode_content_status token_raw_segments_status;
  bag_text_follow_raw_display_unit_entry* token_raw_display_units_buffer;
  size_t token_raw_display_units_buffer_count;
  size_t token_raw_display_units_count;
  bag_decode_content_status token_raw_display_units_status;
  char* lyric_lines_buffer;
  size_t lyric_lines_buffer_size;
  size_t lyric_lines_size;
  bag_decode_content_status lyric_lines_status;
  bag_text_follow_lyric_line_entry* lyric_line_timeline_buffer;
  size_t lyric_line_timeline_buffer_count;
  size_t lyric_line_timeline_count;
  bag_decode_content_status lyric_line_timeline_status;
  bag_text_follow_line_token_range_entry* line_token_ranges_buffer;
  size_t line_token_ranges_buffer_count;
  size_t line_token_ranges_count;
  bag_decode_content_status line_token_ranges_status;
  bag_text_follow_line_raw_segment_entry* line_raw_segments_buffer;
  size_t line_raw_segments_buffer_count;
  size_t line_raw_segments_count;
  bag_decode_content_status line_raw_segments_status;
  int available;
} bag_text_follow_data;

typedef struct bag_decode_result {
  char* text_buffer;
  size_t text_buffer_size;
  size_t text_size;
  char* raw_bytes_hex_buffer;
  size_t raw_bytes_hex_buffer_size;
  size_t raw_bytes_hex_size;
  char* raw_bits_binary_buffer;
  size_t raw_bits_binary_buffer_size;
  size_t raw_bits_binary_size;
  int complete;
  float confidence;
  bag_transport_mode mode;
  bag_decode_content_status text_decode_status;
  bag_decode_content_status raw_bytes_hex_status;
  bag_decode_content_status raw_bits_binary_status;
  int raw_payload_available;
  bag_payload_follow_data follow_data;
  bag_text_follow_data text_follow_data;
} bag_decode_result;

typedef struct bag_pcm16_result {
  int16_t* samples;
  size_t sample_count;
} bag_pcm16_result;

typedef struct bag_encode_result {
  int16_t* samples;
  size_t sample_count;
  char* raw_bytes_hex_buffer;
  size_t raw_bytes_hex_buffer_size;
  size_t raw_bytes_hex_size;
  char* raw_bits_binary_buffer;
  size_t raw_bits_binary_buffer_size;
  size_t raw_bits_binary_size;
  bag_decode_content_status raw_bytes_hex_status;
  bag_decode_content_status raw_bits_binary_status;
  int raw_payload_available;
  bag_payload_follow_data follow_data;
  bag_text_follow_data text_follow_data;
} bag_encode_result;

typedef struct bag_flash_signal_info {
  char* low_carrier_hz_buffer;
  size_t low_carrier_hz_buffer_size;
  size_t low_carrier_hz_size;
  bag_decode_content_status low_carrier_hz_status;
  char* high_carrier_hz_buffer;
  size_t high_carrier_hz_buffer_size;
  size_t high_carrier_hz_size;
  bag_decode_content_status high_carrier_hz_status;
  char* bit_duration_samples_buffer;
  size_t bit_duration_samples_buffer_size;
  size_t bit_duration_samples_size;
  bag_decode_content_status bit_duration_samples_status;
  char* payload_silence_buffer;
  size_t payload_silence_buffer_size;
  size_t payload_silence_size;
  bag_decode_content_status payload_silence_status;
  char* decode_path_buffer;
  size_t decode_path_buffer_size;
  size_t decode_path_size;
  bag_decode_content_status decode_path_status;
  int available;
} bag_flash_signal_info;

typedef struct bag_encode_result_layout {
  size_t sample_count;
  size_t raw_bytes_hex_size;
  size_t raw_bits_binary_size;
  int raw_payload_available;
  size_t byte_timeline_count;
  size_t binary_group_timeline_count;
  size_t text_tokens_size;
  size_t text_token_timeline_count;
  size_t token_raw_segments_count;
  size_t token_raw_display_units_count;
  size_t lyric_lines_size;
  size_t lyric_line_timeline_count;
  size_t line_token_ranges_count;
  size_t line_raw_segments_count;
  int follow_available;
  int text_follow_available;
} bag_encode_result_layout;

typedef enum bag_encode_job_state {
  BAG_ENCODE_JOB_QUEUED = 0,
  BAG_ENCODE_JOB_RUNNING = 1,
  BAG_ENCODE_JOB_SUCCEEDED = 2,
  BAG_ENCODE_JOB_FAILED = 3,
  BAG_ENCODE_JOB_CANCELLED = 4
} bag_encode_job_state;

typedef enum bag_encode_job_phase {
  BAG_ENCODE_JOB_PHASE_PREPARING_INPUT = 0,
  BAG_ENCODE_JOB_PHASE_RENDERING_PCM = 1,
  BAG_ENCODE_JOB_PHASE_POSTPROCESSING = 2,
  BAG_ENCODE_JOB_PHASE_FINALIZING = 3
} bag_encode_job_phase;

typedef struct bag_encode_job_progress {
  bag_encode_job_state state;
  bag_encode_job_phase phase;
  float progress_0_to_1;
  bag_error_code terminal_code;
} bag_encode_job_progress;

const char* bag_transport_mode_name(bag_transport_mode mode);
int bag_try_parse_transport_mode(const char* raw_mode,
                                 bag_transport_mode* out_mode);
bag_validation_issue bag_validate_encode_request(
    const bag_encoder_config* config, const char* text);
bag_validation_issue bag_validate_decoder_config(
    const bag_decoder_config* config);
const char* bag_validation_issue_message(bag_validation_issue issue);
const char* bag_error_code_message(bag_error_code code);

bag_error_code bag_encode_text(const bag_encoder_config* config,
                               const char* text, bag_pcm16_result* out_result);
bag_error_code bag_encode_text_with_follow(const bag_encoder_config* config,
                                           const char* text,
                                           bag_encode_result* out_result);
bag_error_code bag_build_encode_follow_data(
    const bag_encoder_config* config, const char* text,
    bag_encode_result* out_result);
bag_error_code bag_describe_flash_signal(const bag_encoder_config* config,
                                         const char* text,
                                         bag_flash_signal_info* out_info);
bag_error_code bag_start_encode_text_job(const bag_encoder_config* config,
                                         const char* text,
                                         bag_encode_job** out_job);
bag_error_code bag_poll_encode_text_job(const bag_encode_job* job,
                                        bag_encode_job_progress* out_progress);
bag_error_code bag_cancel_encode_text_job(bag_encode_job* job);
bag_error_code bag_take_encode_text_job_result(const bag_encode_job* job,
                                               bag_pcm16_result* out_result);
bag_error_code bag_peek_encode_text_job_result_layout(
    const bag_encode_job* job, bag_encode_result_layout* out_layout);
bag_error_code bag_take_encode_text_job_result_with_follow(
    const bag_encode_job* job, bag_encode_result* out_result);
void bag_destroy_encode_text_job(bag_encode_job* job);
void bag_free_pcm16_result(bag_pcm16_result* result);
void bag_free_encode_result(bag_encode_result* result);

bag_error_code bag_create_decoder(const bag_decoder_config* config,
                                  bag_decoder** out_decoder);
void bag_destroy_decoder(bag_decoder* decoder);

bag_error_code bag_push_pcm(bag_decoder* decoder, const int16_t* samples,
                            size_t sample_count, int64_t timestamp_ms);

bag_error_code bag_poll_decode_result(bag_decoder* decoder,
                                      bag_decode_result* out_result);
bag_error_code bag_poll_result(bag_decoder* decoder,
                               bag_text_result* out_result);
void bag_reset(bag_decoder* decoder);
const char* bag_core_version(void);

#ifdef __cplusplus
}
#endif
