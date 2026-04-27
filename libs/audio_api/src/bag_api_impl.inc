struct bag_decoder {
  std::unique_ptr<bag::ITransportDecoder> decoder;
};

struct bag_encode_job {
  bag::CoreConfig core_config;
  std::string text;
  mutable std::mutex result_mutex;
  bag::EncodedPcmFollowResult result;
  bool follow_result_ready = false;
  std::atomic<bag_encode_job_state> state = BAG_ENCODE_JOB_QUEUED;
  std::atomic<bag_encode_job_phase> phase =
      BAG_ENCODE_JOB_PHASE_PREPARING_INPUT;
  std::atomic<float> progress_0_to_1 = 0.0f;
  std::atomic<bag_error_code> terminal_code = BAG_NOT_READY;
  std::atomic<bool> cancel_requested = false;
  // The repo is moving on a C++23 toolchain, but this job stays on
  // std::thread + cancel_requested because the actual Android NDK/libc++
  // environment is not consistently reliable for std::jthread/stop_token.
  std::thread worker;
};

namespace {
inline constexpr const char kModeFlash[] = "flash";
inline constexpr const char kModePro[] = "pro";
inline constexpr const char kModeUltra[] = "ultra";

std::string FormatRawBytesHex(const std::vector<std::uint8_t>& bytes);
std::string FormatRawBitsBinary(const std::vector<std::uint8_t>& bytes);
bag_decode_content_status CopyStringToApiBuffer(std::string_view input,
                                                char* buffer,
                                                std::size_t buffer_size);
std::string JoinTextTokens(const std::vector<std::string>& tokens);
std::size_t FormattedRawBytesHexSize(const std::vector<std::uint8_t>& bytes);
std::size_t FormattedRawBitsBinarySize(const std::vector<std::uint8_t>& bytes);
std::size_t JoinedTextTokensSize(const std::vector<std::string>& tokens);
bag_decode_content_status CopyRawBytesHexToApi(
    const std::vector<std::uint8_t>& bytes, char* buffer,
    std::size_t buffer_size);
bag_decode_content_status CopyRawBitsBinaryToApi(
    const std::vector<std::uint8_t>& bytes, char* buffer,
    std::size_t buffer_size);
bag_decode_content_status CopyJoinedTextTokensToApi(
    const std::vector<std::string>& tokens, char* buffer,
    std::size_t buffer_size);
void FillEncodeResultLayout(const bag::EncodedPcmFollowResult& result,
                            bag_encode_result_layout* out_layout);
bag_error_code BuildEncodeFollowDataResult(const bag::CoreConfig& core_config,
                                           const std::string& text,
                                           bag::EncodedPcmFollowResult* result);
bag_error_code EnsureEncodeJobFollowResult(bag_encode_job* job);

bag::CoreConfig ToCoreConfig(int sample_rate_hz, int frame_samples,
                             int enable_diagnostics, bag_transport_mode mode,
                             bag_flash_signal_profile flash_signal_profile,
                             bag_flash_voicing_flavor flash_voicing_flavor,
                             int reserved) {
  bag::CoreConfig core_config{};
  core_config.sample_rate_hz = sample_rate_hz;
  core_config.frame_samples = frame_samples;
  core_config.enable_diagnostics = enable_diagnostics != 0;
  switch (mode) {
    case BAG_TRANSPORT_FLASH:
      core_config.mode = bag::TransportMode::kFlash;
      break;
    case BAG_TRANSPORT_PRO:
      core_config.mode = bag::TransportMode::kPro;
      break;
    case BAG_TRANSPORT_ULTRA:
      core_config.mode = bag::TransportMode::kUltra;
      break;
    default:
      core_config.mode = static_cast<bag::TransportMode>(-1);
      break;
  }
  switch (flash_signal_profile) {
    case BAG_FLASH_SIGNAL_PROFILE_CODED_BURST:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kCodedBurst;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_DEEP_RITUAL:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kDeepRitual;
      break;
    default:
      core_config.flash_signal_profile =
          static_cast<bag::FlashSignalProfile>(-1);
      break;
  }
  switch (flash_voicing_flavor) {
    case BAG_FLASH_VOICING_FLAVOR_CODED_BURST:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kCodedBurst;
      break;
    case BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
      break;
    case BAG_FLASH_VOICING_FLAVOR_DEEP_RITUAL:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kDeepRitual;
      break;
    default:
      core_config.flash_voicing_flavor =
          static_cast<bag::FlashVoicingFlavor>(-1);
      break;
  }
  core_config.reserved = reserved;
  return core_config;
}

bag_transport_mode ToApiMode(bag::TransportMode mode) {
  switch (mode) {
    case bag::TransportMode::kFlash:
      return BAG_TRANSPORT_FLASH;
    case bag::TransportMode::kPro:
      return BAG_TRANSPORT_PRO;
    case bag::TransportMode::kUltra:
      return BAG_TRANSPORT_ULTRA;
    default:
      return BAG_TRANSPORT_FLASH;
  }
}

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

void UpdateMonotonicProgress(std::atomic<float>* progress_0_to_1,
                             float candidate) {
  if (progress_0_to_1 == nullptr) {
    return;
  }

  candidate = std::clamp(candidate, 0.0f, 1.0f);
  float current = progress_0_to_1->load();
  while (candidate > current &&
         !progress_0_to_1->compare_exchange_weak(current, candidate)) {
  }
}

void OnEncodeJobProgress(void* user_data, bag::EncodeProgressPhase phase,
                         float progress_0_to_1) {
  auto* job = static_cast<bag_encode_job*>(user_data);
  if (job == nullptr) {
    return;
  }

  job->phase.store(ToApiJobPhase(phase));
  UpdateMonotonicProgress(&job->progress_0_to_1, progress_0_to_1);
}

bool ShouldCancelEncodeJob(void* user_data) {
  auto* job = static_cast<bag_encode_job*>(user_data);
  return job != nullptr && job->cancel_requested.load();
}

// Keep marshalling and async job helpers in dedicated include units so this
// file can stay focused on ABI-facing config/error translation and entrypoints.
#include "bag_api_result_marshalling_impl.inc"
#include "bag_api_encode_job_support_impl.inc"

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
    default:
      return BAG_VALIDATION_INVALID_MODE;
  }
}
}  // namespace

const char* bag_transport_mode_name(bag_transport_mode mode) {
  switch (mode) {
    case BAG_TRANSPORT_FLASH:
      return kModeFlash;
    case BAG_TRANSPORT_PRO:
      return kModePro;
    case BAG_TRANSPORT_ULTRA:
      return kModeUltra;
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
      return "Unsupported transport mode. Use flash, pro, or ultra. Charset "
             "rules: flash accepts unrestricted text bytes, pro requires "
             "ASCII, ultra is for UTF-8 text.";
    case BAG_VALIDATION_PRO_ASCII_ONLY:
      return "`pro` mode only supports ASCII input. Use `flash` for "
             "unrestricted text bytes or `ultra` for UTF-8 text.";
    case BAG_VALIDATION_PAYLOAD_TOO_LARGE:
      return "Payload exceeds the 512-byte single-frame limit.";
    case BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE:
      return "Unsupported flash signal profile. Use coded_burst, "
             "ritual_chant, or deep_ritual.";
    case BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR:
      return "Unsupported flash voicing flavor. Use coded_burst, "
             "ritual_chant, or deep_ritual.";
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

// Split the outward ABI surface by operation family so future refactors can
// move encode and decode flows independently without re-mixing responsibilities.
#include "bag_api_encode_entrypoints_impl.inc"
#include "bag_api_decode_entrypoints_impl.inc"

const char* bag_core_version(void) {
  return bag::CoreVersion();
}
