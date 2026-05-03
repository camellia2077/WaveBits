const char* bag_validation_issue_message(bag_validation_issue issue) {
  switch (issue) {
    case BAG_VALIDATION_OK:
      return "";
    case BAG_VALIDATION_NULL_CONFIG:
      return "Missing configuration.";
    case BAG_VALIDATION_NULL_TEXT:
      return "Missing input text.";
    case BAG_VALIDATION_NULL_DECODER_OUTPUT:
      return "Missing decoder output target.";
    case BAG_VALIDATION_INVALID_SAMPLE_RATE:
      return "Sample rate must be greater than 0.";
    case BAG_VALIDATION_INVALID_FRAME_SAMPLES:
      return "Frame sample count must be greater than 0.";
    case BAG_VALIDATION_INVALID_MODE:
      return "Unsupported transport mode. Use mini, flash, pro, or ultra. "
             "Charset rules: mini accepts Morse-compatible ASCII, flash "
             "accepts unrestricted text bytes, pro requires ASCII, and ultra "
             "is for UTF-8 text.";
    case BAG_VALIDATION_PRO_ASCII_ONLY:
      return "`pro` mode only supports ASCII input. Use `flash` for "
             "unrestricted text bytes or `ultra` for UTF-8 text.";
    case BAG_VALIDATION_PAYLOAD_TOO_LARGE:
      return "Payload exceeds the 512-byte single-frame limit.";
    case BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE:
      return "Unsupported flash signal profile. Use steady, hostile, litany, "
             "collapse, zeal, or void.";
    case BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR:
      return "Unsupported flash voicing flavor. Use steady, hostile, litany, "
             "collapse, zeal, or void.";
    case BAG_VALIDATION_MINI_MORSE_ONLY:
      return "`mini` Morse mode supports A-Z, 0-9, spaces, and common Morse "
             "punctuation.";
    default:
      return "Unknown validation issue.";
  }
}

const char* bag_error_code_message(bag_error_code code) {
  switch (code) {
    case BAG_OK:
      return "OK.";
    case BAG_INVALID_ARGUMENT:
      return "Invalid argument.";
    case BAG_NOT_READY:
      return "Result not ready.";
    case BAG_NOT_IMPLEMENTED:
      return "Not implemented.";
    case BAG_CANCELLED:
      return "Cancelled.";
    case BAG_INTERNAL:
    default:
      return "Internal error.";
  }
}

const char* bag_core_version(void) {
  return bag::CoreVersion();
}
