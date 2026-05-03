bag_error_code ToApiCode(bag::ErrorCode code) {
  switch (code) {
    case bag::ErrorCode::kOk:
      return BAG_OK;
    case bag::ErrorCode::kInvalidArgument:
      return BAG_INVALID_ARGUMENT;
    case bag::ErrorCode::kNotReady:
      return BAG_NOT_READY;
    case bag::ErrorCode::kNotImplemented:
      return BAG_NOT_IMPLEMENTED;
    case bag::ErrorCode::kCancelled:
      return BAG_CANCELLED;
    case bag::ErrorCode::kInternal:
    default:
      return BAG_INTERNAL;
  }
}

bag_decode_content_status ToApiDecodeContentStatus(
    bag::DecodeContentStatus status) {
  switch (status) {
    case bag::DecodeContentStatus::kOk:
      return BAG_DECODE_CONTENT_STATUS_OK;
    case bag::DecodeContentStatus::kUnavailable:
      return BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    case bag::DecodeContentStatus::kInvalidTextPayload:
      return BAG_DECODE_CONTENT_STATUS_INVALID_TEXT_PAYLOAD;
    case bag::DecodeContentStatus::kBufferTooSmall:
      return BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
    case bag::DecodeContentStatus::kInternalError:
    default:
      return BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR;
  }
}

bag_encode_job_phase ToApiJobPhase(bag::EncodeProgressPhase phase) {
  switch (phase) {
    case bag::EncodeProgressPhase::kPreparingInput:
      return BAG_ENCODE_JOB_PHASE_PREPARING_INPUT;
    case bag::EncodeProgressPhase::kRenderingPcm:
      return BAG_ENCODE_JOB_PHASE_RENDERING_PCM;
    case bag::EncodeProgressPhase::kPostprocessing:
      return BAG_ENCODE_JOB_PHASE_POSTPROCESSING;
    case bag::EncodeProgressPhase::kFinalizing:
    default:
      return BAG_ENCODE_JOB_PHASE_FINALIZING;
  }
}

bag_validation_issue ToApiValidationIssue(bag::TransportValidationIssue issue) {
  switch (issue) {
    case bag::TransportValidationIssue::kOk:
      return BAG_VALIDATION_OK;
    case bag::TransportValidationIssue::kInvalidSampleRate:
      return BAG_VALIDATION_INVALID_SAMPLE_RATE;
    case bag::TransportValidationIssue::kInvalidFrameSamples:
      return BAG_VALIDATION_INVALID_FRAME_SAMPLES;
    case bag::TransportValidationIssue::kInvalidMode:
      return BAG_VALIDATION_INVALID_MODE;
    case bag::TransportValidationIssue::kProAsciiOnly:
      return BAG_VALIDATION_PRO_ASCII_ONLY;
    case bag::TransportValidationIssue::kPayloadTooLarge:
      return BAG_VALIDATION_PAYLOAD_TOO_LARGE;
    case bag::TransportValidationIssue::kInvalidFlashSignalProfile:
      return BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE;
    case bag::TransportValidationIssue::kInvalidFlashVoicingFlavor:
      return BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR;
    case bag::TransportValidationIssue::kMiniMorseOnly:
      return BAG_VALIDATION_MINI_MORSE_ONLY;
    default:
      return BAG_VALIDATION_INVALID_MODE;
  }
}
