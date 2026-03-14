struct bag_decoder {
    std::unique_ptr<bag::ITransportDecoder> decoder;
};

namespace {
inline constexpr const char kModeFlash[] = "flash";
inline constexpr const char kModePro[] = "pro";
inline constexpr const char kModeUltra[] = "ultra";

bag::CoreConfig ToCoreConfig(int sample_rate_hz,
                             int frame_samples,
                             int enable_diagnostics,
                             bag_transport_mode mode,
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
    case bag::ErrorCode::kInternal:
    default:
        return BAG_INTERNAL;
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

int bag_try_parse_transport_mode(const char* raw_mode, bag_transport_mode* out_mode) {
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

bag_validation_issue bag_validate_encode_request(const bag_encoder_config* config, const char* text) {
    if (config == nullptr) {
        return BAG_VALIDATION_NULL_CONFIG;
    }
    if (text == nullptr) {
        return BAG_VALIDATION_NULL_TEXT;
    }

    return ToApiValidationIssue(bag::ValidateEncodeRequest(
        ToCoreConfig(
            config->sample_rate_hz,
            config->frame_samples,
            config->enable_diagnostics,
            config->mode,
            config->reserved),
        text));
}

bag_validation_issue bag_validate_decoder_config(const bag_decoder_config* config) {
    if (config == nullptr) {
        return BAG_VALIDATION_NULL_CONFIG;
    }
    return ToApiValidationIssue(bag::ValidateDecoderConfig(ToCoreConfig(
        config->sample_rate_hz,
        config->frame_samples,
        config->enable_diagnostics,
        config->mode,
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
        return "Unsupported transport mode. Use flash, pro, or ultra.";
    case BAG_VALIDATION_PRO_ASCII_ONLY:
        return "`pro` mode only supports ASCII input.";
    case BAG_VALIDATION_PAYLOAD_TOO_LARGE:
        return "Payload exceeds the 512-byte single-frame limit.";
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
    case BAG_INTERNAL:
    default:
        return "Internal error.";
    }
}

bag_error_code bag_encode_text(const bag_encoder_config* config,
                               const char* text,
                               bag_pcm16_result* out_result) {
    if (config == nullptr || text == nullptr || out_result == nullptr) {
        return BAG_INVALID_ARGUMENT;
    }

    out_result->samples = nullptr;
    out_result->sample_count = 0;

    if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
        return BAG_INVALID_ARGUMENT;
    }

    try {
        const bag::CoreConfig core_config = ToCoreConfig(
            config->sample_rate_hz,
            config->frame_samples,
            config->enable_diagnostics,
            config->mode,
            config->reserved);
        std::vector<std::int16_t> pcm;
        const bag::ErrorCode encode_code =
            bag::EncodeTextToPcm16(core_config, text, &pcm);
        if (encode_code != bag::ErrorCode::kOk) {
            return ToApiCode(encode_code);
        }
        if (pcm.empty()) {
            return BAG_OK;
        }

        auto* samples = new (std::nothrow) std::int16_t[pcm.size()];
        if (samples == nullptr) {
            return BAG_INTERNAL;
        }

        std::copy_n(pcm.data(), pcm.size(), samples);
        out_result->samples = samples;
        out_result->sample_count = pcm.size();
        return BAG_OK;
    } catch (const std::invalid_argument&) {
        return BAG_INVALID_ARGUMENT;
    } catch (...) {
        return BAG_INTERNAL;
    }
}

void bag_free_pcm16_result(bag_pcm16_result* result) {
    if (result == nullptr) {
        return;
    }

    delete[] result->samples;
    result->samples = nullptr;
    result->sample_count = 0;
}

bag_error_code bag_create_decoder(const bag_decoder_config* config, bag_decoder** out_decoder) {
    if (config == nullptr || out_decoder == nullptr) {
        return BAG_INVALID_ARGUMENT;
    }

    if (bag_validate_decoder_config(config) != BAG_VALIDATION_OK) {
        return BAG_INVALID_ARGUMENT;
    }

    auto* decoder = new bag_decoder{};
    decoder->decoder = bag::CreateTransportDecoder(ToCoreConfig(
        config->sample_rate_hz,
        config->frame_samples,
        config->enable_diagnostics,
        config->mode,
        config->reserved));
    if (!decoder->decoder) {
        delete decoder;
        return BAG_INTERNAL;
    }

    *out_decoder = decoder;
    return BAG_OK;
}

void bag_destroy_decoder(bag_decoder* decoder) {
    delete decoder;
}

bag_error_code bag_push_pcm(bag_decoder* decoder,
                            const int16_t* samples,
                            size_t sample_count,
                            int64_t timestamp_ms) {
    if (decoder == nullptr || decoder->decoder == nullptr) {
        return BAG_INVALID_ARGUMENT;
    }

    bag::PcmBlock block{};
    block.samples = samples;
    block.sample_count = sample_count;
    block.timestamp_ms = timestamp_ms;
    return ToApiCode(decoder->decoder->PushPcm(block));
}

bag_error_code bag_poll_result(bag_decoder* decoder, bag_text_result* out_result) {
    if (decoder == nullptr || decoder->decoder == nullptr || out_result == nullptr) {
        return BAG_INVALID_ARGUMENT;
    }

    bag::TextResult result{};
    const bag_error_code code = ToApiCode(decoder->decoder->PollTextResult(&result));
    if (code != BAG_OK) {
        if (out_result->buffer != nullptr && out_result->buffer_size > 0) {
            out_result->buffer[0] = '\0';
        }
        out_result->text_size = 0;
        out_result->complete = 0;
        out_result->confidence = 0.0f;
        out_result->mode = BAG_TRANSPORT_FLASH;
        return code;
    }

    out_result->text_size = result.text.size();
    out_result->complete = result.complete ? 1 : 0;
    out_result->confidence = result.confidence;
    out_result->mode = ToApiMode(result.mode);

    if (out_result->buffer != nullptr && out_result->buffer_size > 0) {
        const std::size_t copy_size = std::min(result.text.size(), out_result->buffer_size - 1);
        if (copy_size > 0) {
            std::copy_n(result.text.data(), copy_size, out_result->buffer);
        }
        out_result->buffer[copy_size] = '\0';
    }

    return BAG_OK;
}

void bag_reset(bag_decoder* decoder) {
    if (decoder == nullptr || decoder->decoder == nullptr) {
        return;
    }
    decoder->decoder->Reset();
}

const char* bag_core_version(void) {
    return bag::CoreVersion();
}
