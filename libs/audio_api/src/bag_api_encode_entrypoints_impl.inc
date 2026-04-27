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

bag_error_code bag_start_encode_text_job(const bag_encoder_config* config,
                                         const char* text,
                                         bag_encode_job** out_job) {
  if (config == nullptr || text == nullptr || out_job == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  *out_job = nullptr;
  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    return BAG_INVALID_ARGUMENT;
  }

  bag_encode_job* job = nullptr;
  try {
    job = new (std::nothrow) bag_encode_job{};
    if (job == nullptr) {
      return BAG_INTERNAL;
    }

    job->core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    job->text = text;
    job->worker = std::thread([job]() { RunEncodeTextJobWorker(job); });
    *out_job = job;
    return BAG_OK;
  } catch (...) {
    delete job;
    *out_job = nullptr;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_poll_encode_text_job(const bag_encode_job* job,
                                        bag_encode_job_progress* out_progress) {
  if (job == nullptr || out_progress == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  out_progress->state = job->state.load();
  out_progress->phase = job->phase.load();
  out_progress->progress_0_to_1 =
      std::clamp(job->progress_0_to_1.load(), 0.0f, 1.0f);
  out_progress->terminal_code = job->terminal_code.load();
  return BAG_OK;
}

bag_error_code bag_cancel_encode_text_job(bag_encode_job* job) {
  if (job == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  job->cancel_requested.store(true);
  return BAG_OK;
}

bag_error_code bag_take_encode_text_job_result(const bag_encode_job* job,
                                               bag_pcm16_result* out_result) {
  if (job == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  const bag_encode_job_state state = job->state.load();
  if (state == BAG_ENCODE_JOB_SUCCEEDED) {
    const std::lock_guard lock(job->result_mutex);
    return CopyPcmToApiResult(job->result.pcm, out_result);
  }

  out_result->samples = nullptr;
  out_result->sample_count = 0;
  if (state == BAG_ENCODE_JOB_CANCELLED) {
    return BAG_CANCELLED;
  }
  if (state == BAG_ENCODE_JOB_FAILED) {
    const bag_error_code terminal_code = job->terminal_code.load();
    return terminal_code == BAG_NOT_READY ? BAG_INTERNAL : terminal_code;
  }
  return BAG_NOT_READY;
}

bag_error_code bag_peek_encode_text_job_result_layout(
    const bag_encode_job* job, bag_encode_result_layout* out_layout) {
  if (job == nullptr || out_layout == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  const bag_encode_job_state state = job->state.load();
  if (state == BAG_ENCODE_JOB_SUCCEEDED) {
    const bag_error_code follow_code =
        EnsureEncodeJobFollowResult(const_cast<bag_encode_job*>(job));
    if (follow_code != BAG_OK) {
      *out_layout = {};
      return follow_code;
    }
    const std::lock_guard lock(job->result_mutex);
    FillEncodeResultLayout(job->result, out_layout);
    return BAG_OK;
  }

  *out_layout = {};
  if (state == BAG_ENCODE_JOB_CANCELLED) {
    return BAG_CANCELLED;
  }
  if (state == BAG_ENCODE_JOB_FAILED) {
    const bag_error_code terminal_code = job->terminal_code.load();
    return terminal_code == BAG_NOT_READY ? BAG_INTERNAL : terminal_code;
  }
  return BAG_NOT_READY;
}

bag_error_code bag_take_encode_text_job_result_with_follow(
    const bag_encode_job* job, bag_encode_result* out_result) {
  if (job == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  const bag_encode_job_state state = job->state.load();
  if (state == BAG_ENCODE_JOB_SUCCEEDED) {
    const bag_error_code follow_code =
        EnsureEncodeJobFollowResult(const_cast<bag_encode_job*>(job));
    if (follow_code != BAG_OK) {
      ClearApiEncodeResult(out_result);
      return follow_code;
    }
    const std::lock_guard lock(job->result_mutex);
    return CopyStructuredEncodeResultToApi(job->result, out_result);
  }

  ClearApiEncodeResult(out_result);
  if (state == BAG_ENCODE_JOB_CANCELLED) {
    return BAG_CANCELLED;
  }
  if (state == BAG_ENCODE_JOB_FAILED) {
    const bag_error_code terminal_code = job->terminal_code.load();
    return terminal_code == BAG_NOT_READY ? BAG_INTERNAL : terminal_code;
  }
  return BAG_NOT_READY;
}

void bag_destroy_encode_text_job(bag_encode_job* job) {
  if (job == nullptr) {
    return;
  }

  job->cancel_requested.store(true);
  JoinEncodeJobWorker(job);
  delete job;
}

void bag_free_pcm16_result(bag_pcm16_result* result) {
  if (result == nullptr) {
    return;
  }

  delete[] result->samples;
  result->samples = nullptr;
  result->sample_count = 0;
}

void bag_free_encode_result(bag_encode_result* result) {
  if (result == nullptr) {
    return;
  }

  delete[] result->samples;
  result->samples = nullptr;
  result->sample_count = 0;
  result->raw_bytes_hex_size = 0;
  result->raw_bits_binary_size = 0;
  result->raw_payload_available = 0;
  result->raw_bytes_hex_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  result->raw_bits_binary_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  ClearApiFollowData(&result->follow_data);
  ClearApiTextFollowData(&result->text_follow_data);
}
