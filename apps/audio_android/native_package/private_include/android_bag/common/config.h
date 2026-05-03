#pragma once

#include <cstdint>

namespace bag {

enum class TransportMode : std::uint8_t {
    kMini = 0,
    kFlash = 1,
    kPro = 2,
    kUltra = 3,
};

enum class FlashSignalProfile : std::uint8_t {
    kSteady = 0,
    kLitany = 1,
    kHostile = 3,
    kCollapse = 4,
    kZeal = 5,
    kVoid = 6,
};

enum class FlashVoicingFlavor : std::uint8_t {
    kSteady = 0,
    kLitany = 1,
    kHostile = 3,
    kCollapse = 4,
    kZeal = 5,
    kVoid = 6,
};

inline constexpr bool IsValidTransportMode(TransportMode mode) {
    switch (mode) {
    case TransportMode::kMini:
    case TransportMode::kFlash:
    case TransportMode::kPro:
    case TransportMode::kUltra:
        return true;
    default:
        return false;
    }
}

inline constexpr bool IsFramedTransportMode(TransportMode mode) {
    return mode == TransportMode::kMini || mode == TransportMode::kPro ||
           mode == TransportMode::kUltra;
}

inline constexpr bool IsValidFlashSignalProfile(FlashSignalProfile profile) {
    switch (profile) {
    case FlashSignalProfile::kSteady:
    case FlashSignalProfile::kLitany:
    case FlashSignalProfile::kHostile:
    case FlashSignalProfile::kCollapse:
    case FlashSignalProfile::kZeal:
    case FlashSignalProfile::kVoid:
        return true;
    default:
        return false;
    }
}

inline constexpr bool IsValidFlashVoicingFlavor(FlashVoicingFlavor flavor) {
    switch (flavor) {
    case FlashVoicingFlavor::kSteady:
    case FlashVoicingFlavor::kLitany:
    case FlashVoicingFlavor::kHostile:
    case FlashVoicingFlavor::kCollapse:
    case FlashVoicingFlavor::kZeal:
    case FlashVoicingFlavor::kVoid:
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
    FlashSignalProfile flash_signal_profile = FlashSignalProfile::kSteady;
    FlashVoicingFlavor flash_voicing_flavor = FlashVoicingFlavor::kSteady;
    int reserved = 0;
};

}  // namespace bag
