namespace bag {

using std::int16_t;

namespace {

bool IsAsciiText(std::string_view text) {
  for (unsigned char ch : text) {
    if (ch > 0x7F) {
      return false;
    }
  }
  return true;
}

TransportValidationIssue ValidateCommonConfig(const CoreConfig& config) {
  if (config.sample_rate_hz <= 0) {
    return TransportValidationIssue::kInvalidSampleRate;
  }
  if (config.frame_samples <= 0) {
    return TransportValidationIssue::kInvalidFrameSamples;
  }
  if (!IsValidTransportMode(config.mode)) {
    return TransportValidationIssue::kInvalidMode;
  }
  if (!IsValidFlashSignalProfile(config.flash_signal_profile)) {
    return TransportValidationIssue::kInvalidFlashSignalProfile;
  }
  if (!IsValidFlashVoicingFlavor(config.flash_voicing_flavor)) {
    return TransportValidationIssue::kInvalidFlashVoicingFlavor;
  }
  return TransportValidationIssue::kOk;
}

}  // namespace

TransportValidationIssue ValidateEncodeRequest(const CoreConfig& config,
                                               std::string_view text) {
  const TransportValidationIssue config_issue = ValidateCommonConfig(config);
  if (config_issue != TransportValidationIssue::kOk) {
    return config_issue;
  }

  if (config.mode == TransportMode::kFlash) {
    return TransportValidationIssue::kOk;
  }

  if (config.mode == TransportMode::kPro) {
    return IsAsciiText(text) ? TransportValidationIssue::kOk
                             : TransportValidationIssue::kProAsciiOnly;
  }

  return TransportValidationIssue::kOk;
}

TransportValidationIssue ValidateDecoderConfig(const CoreConfig& config) {
  return ValidateCommonConfig(config);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
    return ErrorCode::kInvalidArgument;
  }

  switch (config.mode) {
    case TransportMode::kFlash:
      return bag::flash::EncodeTextToPcm16(config, text, out_pcm,
                                           progress_sink);
    case TransportMode::kPro:
      return bag::pro::EncodeTextToPcm16(config, text, out_pcm, progress_sink);
    case TransportMode::kUltra:
      return bag::ultra::EncodeTextToPcm16(config, text, out_pcm,
                                           progress_sink);
    default:
      return ErrorCode::kInvalidArgument;
  }
}

ErrorCode EncodeTextToPcm16WithFollowData(const CoreConfig& config,
                                          const std::string& text,
                                          EncodedPcmFollowResult* out_result) {
  return EncodeTextToPcm16WithFollowData(config, text, out_result, nullptr);
}

ErrorCode EncodeTextToPcm16WithFollowData(
    const CoreConfig& config, const std::string& text,
    EncodedPcmFollowResult* out_result,
    const EncodeProgressSink* progress_sink) {
  if (out_result == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<std::uint8_t> raw_payload_bytes;
  ErrorCode payload_code = ErrorCode::kInvalidArgument;
  switch (config.mode) {
    case TransportMode::kFlash:
      payload_code = bag::flash::EncodeTextToBytes(text, &raw_payload_bytes);
      break;
    case TransportMode::kPro:
      payload_code = bag::pro::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    case TransportMode::kUltra:
      payload_code = bag::ultra::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    default:
      return ErrorCode::kInvalidArgument;
  }
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }

  out_result->follow_data = BuildPayloadFollowData(config, raw_payload_bytes);
  out_result->text_follow_data = BuildTextFollowData(out_result->follow_data, text);
  return EncodeTextToPcm16(config, text, &out_result->pcm, progress_sink);
}

ErrorCode BuildEncodeFollowData(const CoreConfig& config,
                                const std::string& text,
                                PayloadFollowData* out_follow_data,
                                TextFollowData* out_text_follow_data) {
  if (out_follow_data == nullptr || out_text_follow_data == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<std::uint8_t> raw_payload_bytes;
  ErrorCode payload_code = ErrorCode::kInvalidArgument;
  switch (config.mode) {
    case TransportMode::kFlash:
      payload_code = bag::flash::EncodeTextToBytes(text, &raw_payload_bytes);
      break;
    case TransportMode::kPro:
      payload_code = bag::pro::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    case TransportMode::kUltra:
      payload_code = bag::ultra::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    default:
      return ErrorCode::kInvalidArgument;
  }
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }

  *out_follow_data = BuildPayloadFollowData(config, raw_payload_bytes);
  *out_text_follow_data = BuildTextFollowData(*out_follow_data, text);
  return ErrorCode::kOk;
}

std::unique_ptr<ITransportDecoder> CreateTransportDecoder(
    const CoreConfig& config) {
  if (ValidateDecoderConfig(config) != TransportValidationIssue::kOk) {
    return nullptr;
  }

  switch (config.mode) {
    case TransportMode::kFlash:
      return bag::flash::CreateDecoder(config);
    case TransportMode::kPro:
      return bag::pro::CreateDecoder(config);
    case TransportMode::kUltra:
      return bag::ultra::CreateDecoder(config);
    default:
      return nullptr;
  }
}

}  // namespace bag
