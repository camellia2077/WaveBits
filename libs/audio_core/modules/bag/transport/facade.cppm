module;

export module bag.transport.facade;

import std;

export import bag.common.config;
export import bag.common.error_code;
export import bag.common.types;
export import bag.transport.decoder;
export import bag.transport.follow;

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
ErrorCode EncodeTextToPcm16WithFollowData(
    const CoreConfig& config, const std::string& text,
    EncodedPcmFollowResult* out_result);
ErrorCode EncodeTextToPcm16WithFollowData(
    const CoreConfig& config, const std::string& text,
    EncodedPcmFollowResult* out_result,
    const EncodeProgressSink* progress_sink);
ErrorCode BuildEncodeFollowData(const CoreConfig& config,
                                const std::string& text,
                                PayloadFollowData* out_follow_data,
                                TextFollowData* out_text_follow_data);
std::unique_ptr<ITransportDecoder> CreateTransportDecoder(
    const CoreConfig& config);

}  // namespace bag
