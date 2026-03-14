module;

import std;

module bag.fsk.codec;

import bag.flash.phy_clean;

namespace bag::fsk {

namespace {

bag::flash::BfskConfig ToBfskConfig(const FskConfig& config) {
    bag::flash::BfskConfig bfsk{};
    bfsk.low_freq_hz = config.low_freq_hz;
    bfsk.high_freq_hz = config.high_freq_hz;
    bfsk.bit_duration_sec = config.bit_duration_sec;
    bfsk.sample_rate_hz = config.sample_rate_hz;
    bfsk.amplitude = config.amplitude;
    return bfsk;
}

}  // namespace

std::vector<std::int16_t> EncodeTextToPcm16(const std::string& text, const FskConfig& config) {
    return bag::flash::EncodeBytesToPcm16(
        std::vector<std::uint8_t>(text.begin(), text.end()),
        ToBfskConfig(config));
}

std::string DecodePcm16ToText(const std::vector<std::int16_t>& pcm, const FskConfig& config) {
    const std::vector<std::uint8_t> bytes =
        bag::flash::DecodePcm16ToBytes(pcm, ToBfskConfig(config));
    return std::string(bytes.begin(), bytes.end());
}

}  // namespace bag::fsk
