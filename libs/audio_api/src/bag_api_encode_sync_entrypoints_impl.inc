bag_error_code bag_encode_text(const bag_encoder_config* config,
                               const char* text, bag_pcm16_result* out_result) {
  if (config == nullptr || text == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    out_result->samples = nullptr;
    out_result->sample_count = 0;
    return BAG_INVALID_ARGUMENT;
  }

  try {
    const bag::CoreConfig core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    std::vector<std::int16_t> pcm;
    const bag::ErrorCode encode_code =
        bag::EncodeTextToPcm16(core_config, text, &pcm);
    if (encode_code != bag::ErrorCode::kOk) {
      out_result->samples = nullptr;
      out_result->sample_count = 0;
      return ToApiCode(encode_code);
    }
    return CopyPcmToApiResult(pcm, out_result);
  } catch (const std::invalid_argument&) {
    out_result->samples = nullptr;
    out_result->sample_count = 0;
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    out_result->samples = nullptr;
    out_result->sample_count = 0;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_encode_text_with_follow(const bag_encoder_config* config,
                                           const char* text,
                                           bag_encode_result* out_result) {
  if (config == nullptr || text == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    ClearApiEncodeResult(out_result);
    return BAG_INVALID_ARGUMENT;
  }

  try {
    const bag::CoreConfig core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    bag::EncodedPcmFollowResult result{};
    const bag::ErrorCode encode_code =
        bag::EncodeTextToPcm16WithFollowData(core_config, text, &result);
    if (encode_code != bag::ErrorCode::kOk) {
      ClearApiEncodeResult(out_result);
      return ToApiCode(encode_code);
    }
    return CopyStructuredEncodeResultToApi(result, out_result);
  } catch (const std::invalid_argument&) {
    ClearApiEncodeResult(out_result);
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    ClearApiEncodeResult(out_result);
    return BAG_INTERNAL;
  }
}

bag_error_code bag_build_encode_follow_data(const bag_encoder_config* config,
                                            const char* text,
                                            bag_encode_result* out_result) {
  if (config == nullptr || text == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    ClearApiEncodeResult(out_result);
    return BAG_INVALID_ARGUMENT;
  }

  try {
    const bag::CoreConfig core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    bag::EncodedPcmFollowResult result{};
    const bag_error_code follow_code =
        BuildEncodeFollowDataResult(core_config, text, &result);
    if (follow_code != BAG_OK) {
      ClearApiEncodeResult(out_result);
      return follow_code;
    }
    return CopyStructuredEncodeResultToApi(result, out_result);
  } catch (const std::invalid_argument&) {
    ClearApiEncodeResult(out_result);
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    ClearApiEncodeResult(out_result);
    return BAG_INTERNAL;
  }
}
