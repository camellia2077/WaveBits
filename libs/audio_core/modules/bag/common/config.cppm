module;

export module bag.common.config;

import std;

export namespace bag {

enum class TransportMode : std::uint8_t {
  kFlash = 0,
  kPro = 1,
  kUltra = 2,
};

enum class FlashSignalProfile : std::uint8_t {
  kCodedBurst = 0,
  kRitualChant = 1,
};

enum class FlashVoicingFlavor : std::uint8_t {
  kCodedBurst = 0,
  kRitualChant = 1,
};

inline constexpr bool IsValidTransportMode(TransportMode mode) {
  switch (mode) {
    case TransportMode::kFlash:
    case TransportMode::kPro:
    case TransportMode::kUltra:
      return true;
    default:
      return false;
  }
}

inline constexpr bool IsFramedTransportMode(TransportMode mode) {
  return mode == TransportMode::kPro || mode == TransportMode::kUltra;
}

inline constexpr bool IsValidFlashSignalProfile(FlashSignalProfile profile) {
  switch (profile) {
    case FlashSignalProfile::kCodedBurst:
    case FlashSignalProfile::kRitualChant:
      return true;
    default:
      return false;
  }
}

inline constexpr bool IsValidFlashVoicingFlavor(FlashVoicingFlavor flavor) {
  switch (flavor) {
    case FlashVoicingFlavor::kCodedBurst:
    case FlashVoicingFlavor::kRitualChant:
      return true;
    default:
      return false;
  }
}

struct CoreConfig {
  int sample_rate_hz = 48000;
  int frame_samples = 480;
  bool enable_diagnostics = false;
  TransportMode mode = TransportMode::kFlash;
  FlashSignalProfile flash_signal_profile = FlashSignalProfile::kCodedBurst;
  FlashVoicingFlavor flash_voicing_flavor = FlashVoicingFlavor::kCodedBurst;
  int reserved = 0;
};

}  // namespace bag
