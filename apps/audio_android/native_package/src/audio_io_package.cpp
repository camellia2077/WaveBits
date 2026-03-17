#include "android_audio_io/audio_io_package.h"

#include "wav_io.h"

#include "../../../../libs/audio_io/src/wav_io_bytes_impl.inc"

namespace android_audio_io {

std::vector<std::uint8_t> EncodeMonoPcm16ToWavBytes(
    int sample_rate_hz,
    const std::vector<std::int16_t>& pcm_samples,
    const EncodedWaveBitsMetadata* metadata
) {
    if (metadata == nullptr) {
        return audio_io::detail::bytes_impl::SerializeMonoPcm16WavBytes(sample_rate_hz, pcm_samples);
    }
    audio_io::WaveBitsAudioMetadata native_metadata{};
    native_metadata.version = metadata->version;
    native_metadata.mode = static_cast<audio_io::WaveBitsAudioMetadataMode>(metadata->mode);
    native_metadata.has_flash_voicing_style = metadata->has_flash_voicing_style;
    native_metadata.flash_voicing_style =
        static_cast<audio_io::WaveBitsAudioMetadataFlashVoicingStyle>(metadata->flash_voicing_style);
    native_metadata.created_at_iso_utc = metadata->created_at_iso_utc;
    native_metadata.duration_ms = metadata->duration_ms;
    native_metadata.frame_samples = metadata->frame_samples;
    native_metadata.pcm_sample_count = metadata->pcm_sample_count;
    native_metadata.app_version = metadata->app_version;
    native_metadata.core_version = metadata->core_version;
    return audio_io::detail::bytes_impl::SerializeMonoPcm16WavBytesWithMetadata(
        sample_rate_hz,
        pcm_samples,
        &native_metadata);
}

DecodedMonoPcm16WavData DecodeMonoPcm16WavBytes(const std::vector<std::uint8_t>& wav_bytes) {
    const auto parsed = audio_io::detail::bytes_impl::ParseMonoPcm16WavBytes(
        wav_bytes.data(),
        wav_bytes.size());
    const auto metadata = audio_io::detail::bytes_impl::ParseWaveBitsAudioMetadataBytes(
        wav_bytes.data(),
        wav_bytes.size());
    return {
        static_cast<WavDecodeStatus>(parsed.status),
        parsed.wav.sample_rate_hz,
        parsed.wav.channels,
        parsed.wav.mono_pcm,
        static_cast<WaveBitsMetadataStatus>(metadata.status),
        EncodedWaveBitsMetadata{
            metadata.metadata.version,
            static_cast<std::uint8_t>(metadata.metadata.mode),
            metadata.metadata.has_flash_voicing_style,
            static_cast<std::uint8_t>(metadata.metadata.flash_voicing_style),
            metadata.metadata.created_at_iso_utc,
            metadata.metadata.duration_ms,
            metadata.metadata.frame_samples,
            metadata.metadata.pcm_sample_count,
            metadata.metadata.app_version,
            metadata.metadata.core_version,
        }
    };
}

}  // namespace android_audio_io
