module;

export module bag.flash.phy_clean;

import std;

export import bag.flash.signal;
export import bag.flash.voicing;
export import bag.common.error_code;
export import bag.transport.decoder;

export namespace bag::flash {

ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(
    const CoreConfig& config, const std::string& text,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(
    const CoreConfig& config, const std::string& text,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::vector<std::int16_t>* out_pcm,
    const EncodeProgressSink* progress_sink);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink);
ErrorCode DecodePcm16ToTextWithSignalProfileAndFlavor(
    const CoreConfig& config, const std::vector<std::int16_t>& pcm,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::string* out_text);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::flash
