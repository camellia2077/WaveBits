#include "audio_io_api.h"

#include <algorithm>
#include <cstring>
#include <new>
#include <string>
#include <vector>

#include "wav_io.h"

namespace {

audio_io_wav_status ToApiStatus(audio_io::WavPcm16Status status) {
  switch (status) {
    case audio_io::WavPcm16Status::kOk:
      return AUDIO_IO_WAV_OK;
    case audio_io::WavPcm16Status::kInvalidArgument:
      return AUDIO_IO_WAV_INVALID_ARGUMENT;
    case audio_io::WavPcm16Status::kInvalidHeader:
      return AUDIO_IO_WAV_INVALID_HEADER;
    case audio_io::WavPcm16Status::kUnsupportedFormat:
      return AUDIO_IO_WAV_UNSUPPORTED_FORMAT;
    case audio_io::WavPcm16Status::kTruncatedData:
      return AUDIO_IO_WAV_TRUNCATED_DATA;
  }

  return AUDIO_IO_WAV_INTERNAL;
}

audio_io_metadata_status ToMetadataApiStatus(
    audio_io::WaveBitsAudioMetadataStatus status) {
  switch (status) {
    case audio_io::WaveBitsAudioMetadataStatus::kOk:
      return AUDIO_IO_METADATA_OK;
    case audio_io::WaveBitsAudioMetadataStatus::kNotFound:
      return AUDIO_IO_METADATA_NOT_FOUND;
    case audio_io::WaveBitsAudioMetadataStatus::kInvalidArgument:
      return AUDIO_IO_METADATA_INVALID_ARGUMENT;
    case audio_io::WaveBitsAudioMetadataStatus::kInvalidHeader:
      return AUDIO_IO_METADATA_INVALID_HEADER;
    case audio_io::WaveBitsAudioMetadataStatus::kUnsupportedVersion:
      return AUDIO_IO_METADATA_UNSUPPORTED_VERSION;
    case audio_io::WaveBitsAudioMetadataStatus::kInvalidMetadata:
      return AUDIO_IO_METADATA_INVALID_METADATA;
    case audio_io::WaveBitsAudioMetadataStatus::kTruncatedData:
      return AUDIO_IO_METADATA_TRUNCATED_DATA;
  }

  return AUDIO_IO_METADATA_INTERNAL;
}

audio_io::WaveBitsAudioMetadataMode ToLibraryMode(audio_io_metadata_mode mode) {
  switch (mode) {
    case AUDIO_IO_METADATA_MODE_FLASH:
      return audio_io::WaveBitsAudioMetadataMode::kFlash;
    case AUDIO_IO_METADATA_MODE_PRO:
      return audio_io::WaveBitsAudioMetadataMode::kPro;
    case AUDIO_IO_METADATA_MODE_ULTRA:
      return audio_io::WaveBitsAudioMetadataMode::kUltra;
    case AUDIO_IO_METADATA_MODE_UNKNOWN:
    default:
      return audio_io::WaveBitsAudioMetadataMode::kUnknown;
  }
}

audio_io_metadata_mode ToApiMode(audio_io::WaveBitsAudioMetadataMode mode) {
  switch (mode) {
    case audio_io::WaveBitsAudioMetadataMode::kFlash:
      return AUDIO_IO_METADATA_MODE_FLASH;
    case audio_io::WaveBitsAudioMetadataMode::kPro:
      return AUDIO_IO_METADATA_MODE_PRO;
    case audio_io::WaveBitsAudioMetadataMode::kUltra:
      return AUDIO_IO_METADATA_MODE_ULTRA;
    case audio_io::WaveBitsAudioMetadataMode::kUnknown:
    default:
      return AUDIO_IO_METADATA_MODE_UNKNOWN;
  }
}

audio_io::WaveBitsAudioMetadataFlashVoicingStyle ToLibraryFlashStyle(
    audio_io_metadata_flash_voicing_style style) {
  switch (style) {
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_CODED_BURST:
      return audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kCodedBurst;
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_RITUAL_CHANT:
      return audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kRitualChant;
    case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN:
    default:
      return audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kUnknown;
  }
}

audio_io_metadata_flash_voicing_style ToApiFlashStyle(
    audio_io::WaveBitsAudioMetadataFlashVoicingStyle style) {
  switch (style) {
    case audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kCodedBurst:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_CODED_BURST;
    case audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kRitualChant:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_RITUAL_CHANT;
    case audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kUnknown:
    default:
      return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
  }
}

bool IsValidStringView(audio_io_string_view value) {
  return value.size == 0 || value.data != nullptr;
}

std::string ToStdString(audio_io_string_view value) {
  if (value.size == 0) {
    return {};
  }
  return std::string(value.data, value.size);
}

void ResetOwnedString(audio_io_owned_string* value) {
  if (value == nullptr) {
    return;
  }
  value->data = nullptr;
  value->size = 0;
}

void ResetMetadata(audio_io_metadata* metadata) {
  if (metadata == nullptr) {
    return;
  }
  metadata->version = 0;
  metadata->mode = AUDIO_IO_METADATA_MODE_UNKNOWN;
  metadata->has_flash_voicing_style = 0;
  metadata->flash_voicing_style = AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
  ResetOwnedString(&metadata->created_at_iso_utc);
  metadata->duration_ms = 0;
  metadata->frame_samples = 0;
  metadata->pcm_sample_count = 0;
  ResetOwnedString(&metadata->app_version);
  ResetOwnedString(&metadata->core_version);
}

void ResetDecodedWav(audio_io_decoded_wav* decoded) {
  if (decoded == nullptr) {
    return;
  }
  decoded->sample_rate_hz = 0;
  decoded->channels = 1;
  decoded->samples = nullptr;
  decoded->sample_count = 0;
  decoded->metadata_status = AUDIO_IO_METADATA_NOT_FOUND;
  ResetMetadata(&decoded->metadata);
}

bool DuplicateOwnedString(const std::string& input, audio_io_owned_string* out) {
  if (out == nullptr) {
    return false;
  }
  ResetOwnedString(out);
  if (input.empty()) {
    return true;
  }

  auto* buffer = new (std::nothrow) char[input.size() + 1u];
  if (buffer == nullptr) {
    return false;
  }
  std::memcpy(buffer, input.data(), input.size());
  buffer[input.size()] = '\0';
  out->data = buffer;
  out->size = input.size();
  return true;
}

bool FillApiMetadata(const audio_io::WaveBitsAudioMetadata& native_metadata,
                     audio_io_metadata* out_metadata) {
  if (out_metadata == nullptr) {
    return false;
  }

  ResetMetadata(out_metadata);
  out_metadata->version = native_metadata.version;
  out_metadata->mode = ToApiMode(native_metadata.mode);
  out_metadata->has_flash_voicing_style =
      native_metadata.has_flash_voicing_style ? 1u : 0u;
  out_metadata->flash_voicing_style =
      native_metadata.has_flash_voicing_style
          ? ToApiFlashStyle(native_metadata.flash_voicing_style)
          : AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
  out_metadata->duration_ms = native_metadata.duration_ms;
  out_metadata->frame_samples = native_metadata.frame_samples;
  out_metadata->pcm_sample_count = native_metadata.pcm_sample_count;

  if (!DuplicateOwnedString(native_metadata.created_at_iso_utc,
                            &out_metadata->created_at_iso_utc) ||
      !DuplicateOwnedString(native_metadata.app_version,
                            &out_metadata->app_version) ||
      !DuplicateOwnedString(native_metadata.core_version,
                            &out_metadata->core_version)) {
    audio_io_free_metadata(out_metadata);
    return false;
  }

  return true;
}

bool ToNativeMetadata(const audio_io_metadata_view* metadata,
                      audio_io::WaveBitsAudioMetadata* out_metadata) {
  if (metadata == nullptr || out_metadata == nullptr) {
    return false;
  }
  if (!IsValidStringView(metadata->created_at_iso_utc) ||
      !IsValidStringView(metadata->app_version) ||
      !IsValidStringView(metadata->core_version)) {
    return false;
  }

  out_metadata->version = metadata->version;
  out_metadata->mode = ToLibraryMode(metadata->mode);
  out_metadata->has_flash_voicing_style =
      metadata->has_flash_voicing_style != 0u;
  out_metadata->flash_voicing_style =
      out_metadata->has_flash_voicing_style
          ? ToLibraryFlashStyle(metadata->flash_voicing_style)
          : audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kUnknown;
  out_metadata->created_at_iso_utc = ToStdString(metadata->created_at_iso_utc);
  out_metadata->duration_ms = metadata->duration_ms;
  out_metadata->frame_samples = metadata->frame_samples;
  out_metadata->pcm_sample_count = metadata->pcm_sample_count;
  out_metadata->app_version = ToStdString(metadata->app_version);
  out_metadata->core_version = ToStdString(metadata->core_version);
  return true;
}

}  // namespace

audio_io_wav_status audio_io_encode_mono_pcm16_wav(
    int sample_rate_hz,
    const int16_t* pcm,
    size_t sample_count,
    audio_io_byte_buffer* out_wav_bytes) {
  if (out_wav_bytes == nullptr) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  out_wav_bytes->data = nullptr;
  out_wav_bytes->size = 0;

  if (sample_rate_hz <= 0 || (sample_count > 0 && pcm == nullptr)) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  try {
    const std::vector<std::int16_t> pcm_samples =
        sample_count > 0
            ? std::vector<std::int16_t>(pcm, pcm + sample_count)
            : std::vector<std::int16_t>{};
    const auto wav_bytes =
        audio_io::SerializeMonoPcm16Wav(sample_rate_hz, pcm_samples);
    if (wav_bytes.empty()) {
      return AUDIO_IO_WAV_INVALID_ARGUMENT;
    }

    auto* buffer = new (std::nothrow) uint8_t[wav_bytes.size()];
    if (buffer == nullptr) {
      return AUDIO_IO_WAV_INTERNAL;
    }
    std::copy(wav_bytes.begin(), wav_bytes.end(), buffer);
    out_wav_bytes->data = buffer;
    out_wav_bytes->size = wav_bytes.size();
    return AUDIO_IO_WAV_OK;
  } catch (...) {
    return AUDIO_IO_WAV_INTERNAL;
  }
}

audio_io_wav_status audio_io_encode_mono_pcm16_wav_with_metadata(
    int sample_rate_hz,
    const int16_t* pcm,
    size_t sample_count,
    const audio_io_metadata_view* metadata,
    audio_io_byte_buffer* out_wav_bytes) {
  if (metadata == nullptr || out_wav_bytes == nullptr) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  out_wav_bytes->data = nullptr;
  out_wav_bytes->size = 0;

  if (sample_rate_hz <= 0 || (sample_count > 0 && pcm == nullptr)) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  try {
    audio_io::WaveBitsAudioMetadata native_metadata{};
    if (!ToNativeMetadata(metadata, &native_metadata)) {
      return AUDIO_IO_WAV_INVALID_ARGUMENT;
    }

    const std::vector<std::int16_t> pcm_samples =
        sample_count > 0
            ? std::vector<std::int16_t>(pcm, pcm + sample_count)
            : std::vector<std::int16_t>{};
    const auto wav_bytes = audio_io::SerializeMonoPcm16WavWithMetadata(
        sample_rate_hz, pcm_samples, native_metadata);
    if (wav_bytes.empty()) {
      return AUDIO_IO_WAV_INVALID_ARGUMENT;
    }

    auto* buffer = new (std::nothrow) uint8_t[wav_bytes.size()];
    if (buffer == nullptr) {
      return AUDIO_IO_WAV_INTERNAL;
    }
    std::copy(wav_bytes.begin(), wav_bytes.end(), buffer);
    out_wav_bytes->data = buffer;
    out_wav_bytes->size = wav_bytes.size();
    return AUDIO_IO_WAV_OK;
  } catch (...) {
    return AUDIO_IO_WAV_INTERNAL;
  }
}

audio_io_wav_status audio_io_decode_mono_pcm16_wav(
    const uint8_t* wav_bytes,
    size_t wav_byte_count,
    audio_io_decoded_wav* out_result) {
  if (out_result == nullptr) {
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  ResetDecodedWav(out_result);

  if (wav_byte_count == 0 || wav_bytes == nullptr) {
    out_result->metadata_status = AUDIO_IO_METADATA_INVALID_ARGUMENT;
    return AUDIO_IO_WAV_INVALID_ARGUMENT;
  }

  try {
    const auto parsed_metadata =
        audio_io::ParseWaveBitsAudioMetadata(wav_bytes, wav_byte_count);
    out_result->metadata_status = ToMetadataApiStatus(parsed_metadata.status);
    if (parsed_metadata.status == audio_io::WaveBitsAudioMetadataStatus::kOk &&
        !FillApiMetadata(parsed_metadata.metadata, &out_result->metadata)) {
      out_result->metadata_status = AUDIO_IO_METADATA_INTERNAL;
    }

    const auto parsed_wav = audio_io::ParseMonoPcm16Wav(wav_bytes, wav_byte_count);
    if (parsed_wav.status != audio_io::WavPcm16Status::kOk) {
      return ToApiStatus(parsed_wav.status);
    }

    const auto& pcm_samples = parsed_wav.wav.mono_pcm;
    auto* buffer = new (std::nothrow) int16_t[pcm_samples.size()];
    if (buffer == nullptr && !pcm_samples.empty()) {
      audio_io_free_metadata(&out_result->metadata);
      out_result->metadata_status = AUDIO_IO_METADATA_INTERNAL;
      return AUDIO_IO_WAV_INTERNAL;
    }
    if (!pcm_samples.empty()) {
      std::copy(pcm_samples.begin(), pcm_samples.end(), buffer);
    }

    out_result->sample_rate_hz = parsed_wav.wav.sample_rate_hz;
    out_result->channels = parsed_wav.wav.channels;
    out_result->samples = buffer;
    out_result->sample_count = pcm_samples.size();
    return AUDIO_IO_WAV_OK;
  } catch (...) {
    audio_io_free_decoded_wav(out_result);
    return AUDIO_IO_WAV_INTERNAL;
  }
}

const char* audio_io_wav_status_message(audio_io_wav_status status) {
  switch (status) {
    case AUDIO_IO_WAV_OK:
      return "WAV operation completed successfully.";
    case AUDIO_IO_WAV_INVALID_ARGUMENT:
      return "Invalid WAV input argument.";
    case AUDIO_IO_WAV_INVALID_HEADER:
      return "Invalid WAV header.";
    case AUDIO_IO_WAV_UNSUPPORTED_FORMAT:
      return "Unsupported WAV format.";
    case AUDIO_IO_WAV_TRUNCATED_DATA:
      return "Truncated WAV data.";
    case AUDIO_IO_WAV_INTERNAL:
      return "Internal WAV I/O failure.";
  }

  return "Unknown WAV I/O status.";
}

const char* audio_io_metadata_status_message(audio_io_metadata_status status) {
  switch (status) {
    case AUDIO_IO_METADATA_OK:
      return "WaveBits metadata parsed successfully.";
    case AUDIO_IO_METADATA_NOT_FOUND:
      return "WaveBits metadata was not found in the WAV file.";
    case AUDIO_IO_METADATA_INVALID_ARGUMENT:
      return "Invalid WaveBits metadata parse argument.";
    case AUDIO_IO_METADATA_INVALID_HEADER:
      return "Invalid WaveBits metadata header.";
    case AUDIO_IO_METADATA_UNSUPPORTED_VERSION:
      return "Unsupported WaveBits metadata version.";
    case AUDIO_IO_METADATA_INVALID_METADATA:
      return "Invalid WaveBits metadata payload.";
    case AUDIO_IO_METADATA_TRUNCATED_DATA:
      return "Truncated WaveBits metadata payload.";
    case AUDIO_IO_METADATA_INTERNAL:
      return "Internal WaveBits metadata failure.";
  }

  return "Unknown WaveBits metadata status.";
}

void audio_io_free_byte_buffer(audio_io_byte_buffer* buffer) {
  if (buffer == nullptr) {
    return;
  }

  delete[] buffer->data;
  buffer->data = nullptr;
  buffer->size = 0;
}

void audio_io_free_metadata(audio_io_metadata* metadata) {
  if (metadata == nullptr) {
    return;
  }

  delete[] metadata->created_at_iso_utc.data;
  delete[] metadata->app_version.data;
  delete[] metadata->core_version.data;
  ResetMetadata(metadata);
}

void audio_io_free_decoded_wav(audio_io_decoded_wav* decoded) {
  if (decoded == nullptr) {
    return;
  }

  delete[] decoded->samples;
  decoded->samples = nullptr;
  decoded->sample_count = 0;
  decoded->sample_rate_hz = 0;
  decoded->channels = 1;
  audio_io_free_metadata(&decoded->metadata);
  decoded->metadata_status = AUDIO_IO_METADATA_NOT_FOUND;
}
