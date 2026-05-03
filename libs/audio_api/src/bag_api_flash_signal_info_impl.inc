namespace {

std::string FormatFrequencyValue(double hz) {
  std::ostringstream stream;
  stream << std::fixed << std::setprecision(0) << hz;
  return stream.str();
}

std::string FormatSampleCountValue(std::size_t samples) {
  return std::to_string(samples);
}

template <typename T>
std::string JoinSortedValues(std::vector<T> values,
                             std::string (*formatter)(T)) {
  if (values.empty()) {
    return "none";
  }
  std::sort(values.begin(), values.end());
  values.erase(std::unique(values.begin(), values.end()), values.end());
  std::string out;
  for (const T value : values) {
    if (!out.empty()) {
      out.append(" / ");
    }
    out.append(formatter(value));
  }
  return out;
}

std::string DecodePathForFlashVoicing(bag::FlashVoicingFlavor flavor) {
  switch (flavor) {
    case bag::FlashVoicingFlavor::kLitany:
    case bag::FlashVoicingFlavor::kCollapse:
      return "gap-aware silence skip";
    case bag::FlashVoicingFlavor::kZeal:
      return "variable-window gap-aware";
    case bag::FlashVoicingFlavor::kSteady:
    case bag::FlashVoicingFlavor::kHostile:
    case bag::FlashVoicingFlavor::kVoid:
    default:
      return "fixed low/high window";
  }
}

std::string PayloadSilenceSummary(const bag::flash::FlashPayloadLayout& layout,
                                  const bag::flash::BfskConfig& signal_config) {
  std::vector<std::size_t> slot_counts;
  for (const bag::flash::FlashPayloadChunk& chunk : layout.chunks) {
    if (chunk.kind != bag::flash::FlashPayloadSegmentKind::kSilence ||
        chunk.sample_count == 0) {
      continue;
    }
    const std::size_t slot_size =
        signal_config.samples_per_silence_slot > 0
            ? signal_config.samples_per_silence_slot
            : chunk.sample_count;
    slot_counts.push_back(std::max(static_cast<std::size_t>(1),
                                   chunk.sample_count / slot_size));
  }
  if (slot_counts.empty()) {
    return "none";
  }
  return JoinSortedValues<std::size_t>(std::move(slot_counts),
                                       FormatSampleCountValue) +
         " slot gap";
}

bag_decode_content_status CopyFlashInfoField(const std::string& value,
                                             char* buffer,
                                             std::size_t buffer_size) {
  return CopyStringToApiBuffer(value, buffer, buffer_size);
}

}  // namespace

bag_error_code bag_describe_flash_signal(const bag_encoder_config* config,
                                         const char* text,
                                         bag_flash_signal_info* out_info) {
  if (config == nullptr || text == nullptr || out_info == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }
  ClearApiFlashSignalInfo(out_info);
  if (config->mode != BAG_TRANSPORT_FLASH) {
    return BAG_INVALID_ARGUMENT;
  }
  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    const bag::CoreConfig core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    std::vector<std::uint8_t> bytes;
    if (bag::flash::EncodeTextToBytes(text, &bytes) != bag::ErrorCode::kOk) {
      return BAG_INVALID_ARGUMENT;
    }
    const bag::flash::BfskConfig signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(
            core_config, core_config.flash_signal_profile);
    const bag::flash::FlashPayloadLayout layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            bytes, signal_config, core_config.flash_voicing_flavor);

    std::vector<double> low_carriers;
    std::vector<double> high_carriers;
    std::vector<std::size_t> bit_samples;
    for (const bag::flash::FlashPayloadChunk& chunk : layout.chunks) {
      if (chunk.kind != bag::flash::FlashPayloadSegmentKind::kBit) {
        continue;
      }
      bit_samples.push_back(chunk.sample_count);
      if (chunk.bit_value == 0) {
        low_carriers.push_back(chunk.carrier_freq_hz);
      } else {
        high_carriers.push_back(chunk.carrier_freq_hz);
      }
    }
    if (low_carriers.empty()) {
      low_carriers.push_back(signal_config.low_freq_hz);
    }
    if (high_carriers.empty()) {
      high_carriers.push_back(signal_config.high_freq_hz);
    }

    const std::string low_text =
        JoinSortedValues<double>(std::move(low_carriers), FormatFrequencyValue);
    const std::string high_text =
        JoinSortedValues<double>(std::move(high_carriers), FormatFrequencyValue);
    const std::string bit_text =
        JoinSortedValues<std::size_t>(std::move(bit_samples),
                                      FormatSampleCountValue);
    const std::string silence_text = PayloadSilenceSummary(layout, signal_config);
    const std::string decode_text =
        DecodePathForFlashVoicing(core_config.flash_voicing_flavor);

    out_info->low_carrier_hz_size = low_text.size();
    out_info->high_carrier_hz_size = high_text.size();
    out_info->bit_duration_samples_size = bit_text.size();
    out_info->payload_silence_size = silence_text.size();
    out_info->decode_path_size = decode_text.size();
    out_info->low_carrier_hz_status = CopyFlashInfoField(
        low_text, out_info->low_carrier_hz_buffer,
        out_info->low_carrier_hz_buffer_size);
    out_info->high_carrier_hz_status = CopyFlashInfoField(
        high_text, out_info->high_carrier_hz_buffer,
        out_info->high_carrier_hz_buffer_size);
    out_info->bit_duration_samples_status = CopyFlashInfoField(
        bit_text, out_info->bit_duration_samples_buffer,
        out_info->bit_duration_samples_buffer_size);
    out_info->payload_silence_status = CopyFlashInfoField(
        silence_text, out_info->payload_silence_buffer,
        out_info->payload_silence_buffer_size);
    out_info->decode_path_status = CopyFlashInfoField(
        decode_text, out_info->decode_path_buffer,
        out_info->decode_path_buffer_size);
    out_info->available = 1;
    return BAG_OK;
  } catch (const std::invalid_argument&) {
    ClearApiFlashSignalInfo(out_info);
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    ClearApiFlashSignalInfo(out_info);
    return BAG_INTERNAL;
  }
}
