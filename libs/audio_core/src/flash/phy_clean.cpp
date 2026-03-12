#if !defined(WAVEBITS_MODULE_IMPL_WRAPPER)
#if __cplusplus >= 202002L
module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>
#endif

module bag.flash.phy_clean;

import bag.flash.codec;
#else
#include "bag/legacy/flash/phy_clean.h"

#include <algorithm>
#include <cmath>
#include <stdexcept>

#include "bag/legacy/flash/codec.h"
#endif
#endif

namespace bag::flash {

using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {

int ChunkSize(const BfskConfig& config) {
    return static_cast<int>(config.sample_rate_hz * config.bit_duration_sec);
}

std::vector<int> BytesToBits(const std::vector<uint8_t>& bytes) {
    std::vector<int> bits;
    bits.reserve(bytes.size() * 8);
    for (uint8_t value : bytes) {
        for (int shift = 7; shift >= 0; --shift) {
            bits.push_back((value >> shift) & 0x01U);
        }
    }
    return bits;
}

std::vector<uint8_t> BitsToBytes(const std::vector<int>& bits) {
    std::vector<uint8_t> bytes;
    bytes.reserve(bits.size() / 8);
    for (size_t index = 0; index + 7 < bits.size(); index += 8) {
        uint8_t value = 0;
        for (int bit = 0; bit < 8; ++bit) {
            value = static_cast<uint8_t>((value << 1) | (bits[index + static_cast<size_t>(bit)] ? 1 : 0));
        }
        bytes.push_back(value);
    }
    return bytes;
}

double GoertzelPower(const int16_t* chunk,
                     int chunk_size,
                     int sample_rate_hz,
                     double target_freq_hz) {
    constexpr double kPi = 3.14159265358979323846;
    const double normalized_freq = target_freq_hz / static_cast<double>(sample_rate_hz);
    const double omega = 2.0 * kPi * normalized_freq;
    const double coeff = 2.0 * std::cos(omega);

    double q0 = 0.0;
    double q1 = 0.0;
    double q2 = 0.0;
    for (int index = 0; index < chunk_size; ++index) {
        const double sample = static_cast<double>(chunk[static_cast<size_t>(index)]) / 32767.0;
        q0 = coeff * q1 - q2 + sample;
        q2 = q1;
        q1 = q0;
    }
    return q1 * q1 + q2 * q2 - coeff * q1 * q2;
}

class FlashDecoder final : public ITransportDecoder {
public:
    explicit FlashDecoder(CoreConfig config) : config_(config) {}

    ErrorCode PushPcm(const PcmBlock& block) override {
        if (block.samples == nullptr || block.sample_count == 0) {
            return ErrorCode::kInvalidArgument;
        }
        buffered_pcm_.insert(buffered_pcm_.end(), block.samples, block.samples + block.sample_count);
        has_pending_result_ = true;
        return ErrorCode::kOk;
    }

    ErrorCode PollTextResult(TextResult* out_result) override {
        if (out_result == nullptr) {
            return ErrorCode::kInvalidArgument;
        }
        if (!has_pending_result_ || buffered_pcm_.empty()) {
            out_result->text.clear();
            out_result->complete = false;
            out_result->confidence = 0.0f;
            out_result->mode = TransportMode::kFlash;
            return ErrorCode::kNotReady;
        }

        std::string decoded_text;
        const ErrorCode decode_code = DecodePcm16ToText(config_, buffered_pcm_, &decoded_text);
        if (decode_code != ErrorCode::kOk) {
            out_result->text.clear();
            out_result->complete = false;
            out_result->confidence = 0.0f;
            out_result->mode = TransportMode::kFlash;
            return ErrorCode::kInternal;
        }

        out_result->text = decoded_text;
        out_result->complete = true;
        out_result->confidence = 1.0f;
        out_result->mode = TransportMode::kFlash;
        has_pending_result_ = false;
        return ErrorCode::kOk;
    }

    void Reset() override {
        buffered_pcm_.clear();
        has_pending_result_ = false;
    }

private:
    CoreConfig config_;
    std::vector<int16_t> buffered_pcm_;
    bool has_pending_result_ = false;
};

}  // namespace

BfskConfig MakeBfskConfig(const CoreConfig& config) {
    BfskConfig bfsk{};
    bfsk.sample_rate_hz = config.sample_rate_hz;
    bfsk.bit_duration_sec =
        static_cast<double>(config.frame_samples) / static_cast<double>(config.sample_rate_hz);
    return bfsk;
}

std::vector<int16_t> EncodeBytesToPcm16(const std::vector<uint8_t>& bytes, const BfskConfig& config) {
    const int chunk_size = ChunkSize(config);
    if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
        throw std::invalid_argument("Invalid BFSK config.");
    }

    const std::vector<int> bits = BytesToBits(bytes);
    std::vector<int16_t> pcm;
    pcm.reserve(bits.size() * static_cast<size_t>(chunk_size));

    constexpr double kPi = 3.14159265358979323846;
    for (int bit : bits) {
        const double freq = bit == 1 ? config.high_freq_hz : config.low_freq_hz;
        const double two_pi_f = 2.0 * kPi * freq;
        for (int index = 0; index < chunk_size; ++index) {
            const double time =
                static_cast<double>(index) / static_cast<double>(config.sample_rate_hz);
            const double sample = config.amplitude * std::sin(two_pi_f * time);
            const double clamped = std::max(-1.0, std::min(1.0, sample));
            pcm.push_back(static_cast<int16_t>(clamped * 32767.0));
        }
    }

    return pcm;
}

std::vector<uint8_t> DecodePcm16ToBytes(const std::vector<int16_t>& pcm, const BfskConfig& config) {
    const int chunk_size = ChunkSize(config);
    if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
        throw std::invalid_argument("Invalid BFSK config.");
    }
    if (pcm.empty()) {
        return {};
    }

    std::vector<int> bits;
    for (size_t offset = 0; offset + static_cast<size_t>(chunk_size) <= pcm.size();
         offset += static_cast<size_t>(chunk_size)) {
        const int16_t* chunk = pcm.data() + offset;
        const double low_power =
            GoertzelPower(chunk, chunk_size, config.sample_rate_hz, config.low_freq_hz);
        const double high_power =
            GoertzelPower(chunk, chunk_size, config.sample_rate_hz, config.high_freq_hz);
        bits.push_back(high_power > low_power ? 1 : 0);
    }

    return BitsToBytes(bits);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<int16_t>* out_pcm) {
    if (out_pcm == nullptr) {
        return ErrorCode::kInvalidArgument;
    }

    std::vector<uint8_t> bytes;
    const ErrorCode bytes_code = EncodeTextToBytes(text, &bytes);
    if (bytes_code != ErrorCode::kOk) {
        return bytes_code;
    }

    try {
        *out_pcm = EncodeBytesToPcm16(bytes, MakeBfskConfig(config));
        return ErrorCode::kOk;
    } catch (...) {
        return ErrorCode::kInvalidArgument;
    }
}

ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<int16_t>& pcm,
                            std::string* out_text) {
    if (out_text == nullptr) {
        return ErrorCode::kInvalidArgument;
    }

    try {
        const std::vector<uint8_t> bytes = DecodePcm16ToBytes(pcm, MakeBfskConfig(config));
        return DecodeBytesToText(bytes, out_text);
    } catch (...) {
        return ErrorCode::kInvalidArgument;
    }
}

std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config) {
    return std::make_unique<FlashDecoder>(config);
}

}  // namespace bag::flash
