const char* bag_transport_mode_name(bag_transport_mode mode) {
  switch (mode) {
    case BAG_TRANSPORT_FLASH:
      return kModeFlash;
    case BAG_TRANSPORT_PRO:
      return kModePro;
    case BAG_TRANSPORT_ULTRA:
      return kModeUltra;
    case BAG_TRANSPORT_MINI:
      return kModeMini;
    default:
      return "unknown";
  }
}

int bag_try_parse_transport_mode(const char* raw_mode,
                                 bag_transport_mode* out_mode) {
  if (raw_mode == nullptr || out_mode == nullptr) {
    return 0;
  }
  const std::string mode_text(raw_mode);
  if (mode_text == kModeFlash) {
    *out_mode = BAG_TRANSPORT_FLASH;
    return 1;
  }
  if (mode_text == kModePro) {
    *out_mode = BAG_TRANSPORT_PRO;
    return 1;
  }
  if (mode_text == kModeUltra) {
    *out_mode = BAG_TRANSPORT_ULTRA;
    return 1;
  }
  if (mode_text == kModeMini) {
    *out_mode = BAG_TRANSPORT_MINI;
    return 1;
  }
  return 0;
}

bag_validation_issue bag_validate_encode_request(
    const bag_encoder_config* config, const char* text) {
  if (config == nullptr) {
    return BAG_VALIDATION_NULL_CONFIG;
  }
  if (text == nullptr) {
    return BAG_VALIDATION_NULL_TEXT;
  }

  return ToApiValidationIssue(bag::ValidateEncodeRequest(
      ToCoreConfig(config->sample_rate_hz, config->frame_samples,
                   config->enable_diagnostics, config->mode,
                   config->flash_signal_profile, config->flash_voicing_flavor,
                   config->reserved),
      text));
}

bag_validation_issue bag_validate_decoder_config(
    const bag_decoder_config* config) {
  if (config == nullptr) {
    return BAG_VALIDATION_NULL_CONFIG;
  }
  return ToApiValidationIssue(bag::ValidateDecoderConfig(ToCoreConfig(
      config->sample_rate_hz, config->frame_samples, config->enable_diagnostics,
      config->mode, config->flash_signal_profile, config->flash_voicing_flavor,
      config->reserved)));
}
