module;

#if !defined(WAVEBITS_CORE_IMPORT_STD)
#include <cstdint>
#include <memory>
#include <string>
#include <string_view>
#include <vector>
#endif

export module bag.transport.facade;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#endif

export import bag.common.config;
export import bag.common.error_code;
export import bag.transport.decoder;

export namespace bag {

enum class TransportValidationIssue {
    kOk = 0,
    kInvalidSampleRate = 1,
    kInvalidFrameSamples = 2,
    kInvalidMode = 3,
    kProAsciiOnly = 4,
    kPayloadTooLarge = 5,
};

TransportValidationIssue ValidateEncodeRequest(const CoreConfig& config, std::string_view text);
TransportValidationIssue ValidateDecoderConfig(const CoreConfig& config);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
std::unique_ptr<ITransportDecoder> CreateTransportDecoder(const CoreConfig& config);

}  // namespace bag
