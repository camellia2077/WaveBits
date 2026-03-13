#if !defined(WAVEBITS_MODULE_IMPL_WRAPPER)
#if __cplusplus >= 202002L
module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>
#endif

module bag.ultra.phy_clean;

import bag.ultra.codec;
#endif
#endif

namespace bag::ultra {

using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {

constexpr double kPi = 3.14159265358979323846;

bool IsValidConfig(const Mfsk16Config& config) {
    return config.sample_rate_hz > 0 && config.symbol_samples > 0 && config.amplitude > 0.0;
}

double GoertzelPower(const int16_t* chunk,
                     int chunk_size,
                     int sample_rate_hz,
                     double target_freq_hz) {
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

size_t FindStrongestSymbol(const int16_t* chunk,
                           int chunk_size,
                           const Mfsk16Config& config) {
    std::array<double, 16> powers{};
    for (size_t index = 0; index < config.freqs_hz.size(); ++index) {
        powers[index] = GoertzelPower(chunk, chunk_size, config.sample_rate_hz, config.freqs_hz[index]);
    }
    return static_cast<size_t>(
        std::distance(powers.begin(), std::max_element(powers.begin(), powers.end())));
}

class UltraDecoder final : public ITransportDecoder {
public:
    explicit UltraDecoder(CoreConfig config) : config_(config) {}

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
            out_result->mode = TransportMode::kUltra;
            return ErrorCode::kNotReady;
        }

        std::string decoded_text;
        const ErrorCode decode_code = DecodePcm16ToText(config_, buffered_pcm_, &decoded_text);
        if (decode_code != ErrorCode::kOk) {
            out_result->text.clear();
            out_result->complete = false;
            out_result->confidence = 0.0f;
            out_result->mode = TransportMode::kUltra;
            return ErrorCode::kInternal;
        }

        out_result->text = decoded_text;
        out_result->complete = true;
        out_result->confidence = 1.0f;
        out_result->mode = TransportMode::kUltra;
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

Mfsk16Config MakeMfsk16Config(const CoreConfig& config) {
    Mfsk16Config out{};
    out.sample_rate_hz = config.sample_rate_hz;
    out.symbol_samples = config.frame_samples;
    return out;
}

ErrorCode EncodeSymbolsToPcm16(const std::vector<uint8_t>& symbols,
                               const Mfsk16Config& config,
                               std::vector<int16_t>* out_pcm) {
    if (out_pcm == nullptr || !IsValidConfig(config)) {
        return ErrorCode::kInvalidArgument;
    }

    out_pcm->clear();
    out_pcm->reserve(symbols.size() * static_cast<size_t>(config.symbol_samples));
    for (uint8_t symbol : symbols) {
        if (symbol > 0x0F) {
            return ErrorCode::kInvalidArgument;
        }

        const double omega = 2.0 * kPi * config.freqs_hz[static_cast<size_t>(symbol)];
        for (int index = 0; index < config.symbol_samples; ++index) {
            const double time =
                static_cast<double>(index) / static_cast<double>(config.sample_rate_hz);
            const double sample = config.amplitude * std::sin(omega * time);
            const double clamped = std::max(-1.0, std::min(1.0, sample));
            out_pcm->push_back(static_cast<int16_t>(clamped * 32767.0));
        }
    }

    return ErrorCode::kOk;
}

ErrorCode DecodePcm16ToSymbols(const std::vector<int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<uint8_t>* out_symbols) {
    if (out_symbols == nullptr || !IsValidConfig(config)) {
        return ErrorCode::kInvalidArgument;
    }
    if (pcm.empty()) {
        out_symbols->clear();
        return ErrorCode::kOk;
    }
    if (pcm.size() % static_cast<size_t>(config.symbol_samples) != 0) {
        return ErrorCode::kInvalidArgument;
    }

    out_symbols->clear();
    out_symbols->reserve(pcm.size() / static_cast<size_t>(config.symbol_samples));
    for (size_t offset = 0; offset < pcm.size(); offset += static_cast<size_t>(config.symbol_samples)) {
        const int16_t* chunk = pcm.data() + offset;
        out_symbols->push_back(static_cast<uint8_t>(
            FindStrongestSymbol(chunk, config.symbol_samples, config)));
    }

    return ErrorCode::kOk;
}

ErrorCode EncodePayloadToPcm16(const std::vector<uint8_t>& payload,
                               const Mfsk16Config& config,
                               std::vector<int16_t>* out_pcm) {
    if (out_pcm == nullptr) {
        return ErrorCode::kInvalidArgument;
    }

    std::vector<uint8_t> symbols;
    const ErrorCode symbols_code = EncodePayloadToSymbols(payload, &symbols);
    if (symbols_code != ErrorCode::kOk) {
        return symbols_code;
    }
    return EncodeSymbolsToPcm16(symbols, config, out_pcm);
}

ErrorCode DecodePcm16ToPayload(const std::vector<int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<uint8_t>* out_payload) {
    if (out_payload == nullptr) {
        return ErrorCode::kInvalidArgument;
    }

    std::vector<uint8_t> symbols;
    const ErrorCode symbols_code = DecodePcm16ToSymbols(pcm, config, &symbols);
    if (symbols_code != ErrorCode::kOk) {
        return symbols_code;
    }
    return DecodeSymbolsToPayload(symbols, out_payload);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<int16_t>* out_pcm) {
    if (out_pcm == nullptr) {
        return ErrorCode::kInvalidArgument;
    }

    std::vector<uint8_t> payload;
    const ErrorCode payload_code = EncodeTextToPayload(text, &payload);
    if (payload_code != ErrorCode::kOk) {
        return payload_code;
    }
    return EncodePayloadToPcm16(payload, MakeMfsk16Config(config), out_pcm);
}

ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<int16_t>& pcm,
                            std::string* out_text) {
    if (out_text == nullptr) {
        return ErrorCode::kInvalidArgument;
    }

    std::vector<uint8_t> payload;
    const ErrorCode payload_code = DecodePcm16ToPayload(pcm, MakeMfsk16Config(config), &payload);
    if (payload_code != ErrorCode::kOk) {
        return payload_code;
    }
    return DecodePayloadToText(payload, out_text);
}

std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config) {
    return std::make_unique<UltraDecoder>(config);
}

}  // namespace bag::ultra
