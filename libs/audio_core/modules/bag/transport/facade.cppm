module;

export module bag.transport.facade;

import std;

export import bag.common.config;
export import bag.common.error_code;
export import bag.common.types;
export import bag.transport.decoder;

export namespace bag {

enum class TransportValidationIssue {
  kOk = 0,
  kInvalidSampleRate = 1,
  kInvalidFrameSamples = 2,
  kInvalidMode = 3,
  kProAsciiOnly = 4,
  kPayloadTooLarge = 5,
  kInvalidFlashSignalProfile = 6,
  kInvalidFlashVoicingFlavor = 7,
};

TransportValidationIssue ValidateEncodeRequest(const CoreConfig& config,
                                               std::string_view text);
TransportValidationIssue ValidateDecoderConfig(const CoreConfig& config);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink);
std::unique_ptr<ITransportDecoder> CreateTransportDecoder(
    const CoreConfig& config);

}  // namespace bag
