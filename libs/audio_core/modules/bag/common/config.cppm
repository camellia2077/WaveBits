module;

export module bag.common.config;

import std;

export namespace bag {

enum class TransportMode : std::uint8_t {
    kFlash = 0,
    kPro = 1,
    kUltra = 2,
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

struct CoreConfig {
    int sample_rate_hz = 48000;
    int frame_samples = 480;
    bool enable_diagnostics = false;
    TransportMode mode = TransportMode::kFlash;
    int reserved = 0;
};

}  // namespace bag
