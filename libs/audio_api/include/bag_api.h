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
    BAG_INTERNAL = 4
} bag_error_code;

typedef enum bag_transport_mode {
    BAG_TRANSPORT_FLASH = 0,
    BAG_TRANSPORT_PRO = 1,
    BAG_TRANSPORT_ULTRA = 2
} bag_transport_mode;

typedef enum bag_validation_issue {
    BAG_VALIDATION_OK = 0,
    BAG_VALIDATION_NULL_CONFIG = 1,
    BAG_VALIDATION_NULL_TEXT = 2,
    BAG_VALIDATION_NULL_DECODER_OUTPUT = 3,
    BAG_VALIDATION_INVALID_SAMPLE_RATE = 4,
    BAG_VALIDATION_INVALID_FRAME_SAMPLES = 5,
    BAG_VALIDATION_INVALID_MODE = 6,
    BAG_VALIDATION_PRO_ASCII_ONLY = 7,
    BAG_VALIDATION_PAYLOAD_TOO_LARGE = 8
} bag_validation_issue;

typedef struct bag_decoder bag_decoder;

typedef struct bag_encoder_config {
    int sample_rate_hz;
    int frame_samples;
    int enable_diagnostics;
    bag_transport_mode mode;
    int reserved;
} bag_encoder_config;

typedef struct bag_decoder_config {
    int sample_rate_hz;
    int frame_samples;
    int enable_diagnostics;
    bag_transport_mode mode;
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

typedef struct bag_pcm16_result {
    int16_t* samples;
    size_t sample_count;
} bag_pcm16_result;

const char* bag_transport_mode_name(bag_transport_mode mode);
int bag_try_parse_transport_mode(const char* raw_mode, bag_transport_mode* out_mode);
bag_validation_issue bag_validate_encode_request(const bag_encoder_config* config, const char* text);
bag_validation_issue bag_validate_decoder_config(const bag_decoder_config* config);
const char* bag_validation_issue_message(bag_validation_issue issue);
const char* bag_error_code_message(bag_error_code code);

bag_error_code bag_encode_text(const bag_encoder_config* config,
                               const char* text,
                               bag_pcm16_result* out_result);
void bag_free_pcm16_result(bag_pcm16_result* result);

bag_error_code bag_create_decoder(const bag_decoder_config* config, bag_decoder** out_decoder);
void bag_destroy_decoder(bag_decoder* decoder);

bag_error_code bag_push_pcm(bag_decoder* decoder,
                            const int16_t* samples,
                            size_t sample_count,
                            int64_t timestamp_ms);

bag_error_code bag_poll_result(bag_decoder* decoder, bag_text_result* out_result);
void bag_reset(bag_decoder* decoder);
const char* bag_core_version(void);

#ifdef __cplusplus
}
#endif
