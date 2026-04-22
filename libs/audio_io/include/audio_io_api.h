#pragma once

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum audio_io_wav_status {
  AUDIO_IO_WAV_OK = 0,
  AUDIO_IO_WAV_INVALID_ARGUMENT = 1,
  AUDIO_IO_WAV_INVALID_HEADER = 2,
  AUDIO_IO_WAV_UNSUPPORTED_FORMAT = 3,
  AUDIO_IO_WAV_TRUNCATED_DATA = 4,
  AUDIO_IO_WAV_INTERNAL = 5
} audio_io_wav_status;

typedef enum audio_io_metadata_status {
  AUDIO_IO_METADATA_OK = 0,
  AUDIO_IO_METADATA_NOT_FOUND = 1,
  AUDIO_IO_METADATA_INVALID_ARGUMENT = 2,
  AUDIO_IO_METADATA_INVALID_HEADER = 3,
  AUDIO_IO_METADATA_UNSUPPORTED_VERSION = 4,
  AUDIO_IO_METADATA_INVALID_METADATA = 5,
  AUDIO_IO_METADATA_TRUNCATED_DATA = 6,
  AUDIO_IO_METADATA_INTERNAL = 7
} audio_io_metadata_status;

typedef enum audio_io_metadata_mode {
  AUDIO_IO_METADATA_MODE_UNKNOWN = 0,
  AUDIO_IO_METADATA_MODE_FLASH = 1,
  AUDIO_IO_METADATA_MODE_PRO = 2,
  AUDIO_IO_METADATA_MODE_ULTRA = 3
} audio_io_metadata_mode;

typedef enum audio_io_metadata_flash_voicing_style {
  AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN = 0,
  AUDIO_IO_METADATA_FLASH_VOICING_STYLE_CODED_BURST = 1,
  AUDIO_IO_METADATA_FLASH_VOICING_STYLE_RITUAL_CHANT = 2,
  AUDIO_IO_METADATA_FLASH_VOICING_STYLE_DEEP_RITUAL = 3
} audio_io_metadata_flash_voicing_style;

typedef struct audio_io_string_view {
  const char* data;
  size_t size;
} audio_io_string_view;

typedef struct audio_io_owned_string {
  char* data;
  size_t size;
} audio_io_owned_string;

typedef struct audio_io_metadata_view {
  uint8_t version;
  audio_io_metadata_mode mode;
  uint8_t has_flash_voicing_style;
  audio_io_metadata_flash_voicing_style flash_voicing_style;
  audio_io_string_view created_at_iso_utc;
  uint32_t duration_ms;
  uint32_t frame_samples;
  uint32_t pcm_sample_count;
  audio_io_string_view app_version;
  audio_io_string_view core_version;
} audio_io_metadata_view;

typedef struct audio_io_metadata {
  uint8_t version;
  audio_io_metadata_mode mode;
  uint8_t has_flash_voicing_style;
  audio_io_metadata_flash_voicing_style flash_voicing_style;
  audio_io_owned_string created_at_iso_utc;
  uint32_t duration_ms;
  uint32_t frame_samples;
  uint32_t pcm_sample_count;
  audio_io_owned_string app_version;
  audio_io_owned_string core_version;
} audio_io_metadata;

typedef struct audio_io_byte_buffer {
  uint8_t* data;
  size_t size;
} audio_io_byte_buffer;

typedef struct audio_io_decoded_wav {
  int sample_rate_hz;
  int channels;
  int16_t* samples;
  size_t sample_count;
  audio_io_metadata_status metadata_status;
  audio_io_metadata metadata;
} audio_io_decoded_wav;

audio_io_wav_status audio_io_encode_mono_pcm16_wav(
    int sample_rate_hz,
    const int16_t* pcm,
    size_t sample_count,
    audio_io_byte_buffer* out_wav_bytes);

audio_io_wav_status audio_io_encode_mono_pcm16_wav_with_metadata(
    int sample_rate_hz,
    const int16_t* pcm,
    size_t sample_count,
    const audio_io_metadata_view* metadata,
    audio_io_byte_buffer* out_wav_bytes);

audio_io_wav_status audio_io_decode_mono_pcm16_wav(
    const uint8_t* wav_bytes,
    size_t wav_byte_count,
    audio_io_decoded_wav* out_result);

const char* audio_io_wav_status_message(audio_io_wav_status status);
const char* audio_io_metadata_status_message(audio_io_metadata_status status);

void audio_io_free_byte_buffer(audio_io_byte_buffer* buffer);
void audio_io_free_metadata(audio_io_metadata* metadata);
void audio_io_free_decoded_wav(audio_io_decoded_wav* decoded);

#ifdef __cplusplus
}
#endif
