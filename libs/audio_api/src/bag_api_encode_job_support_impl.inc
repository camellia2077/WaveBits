void ClearEncodeJobResult(bag_encode_job* job) {
  if (job == nullptr) {
    return;
  }

  const std::lock_guard lock(job->result_mutex);
  job->result = {};
  job->follow_result_ready = false;
}

void CompleteEncodeJobSuccess(bag_encode_job* job,
                              bag::EncodedPcmFollowResult&& result) {
  {
    const std::lock_guard lock(job->result_mutex);
    job->result = std::move(result);
    job->follow_result_ready = job->result.follow_data.available ||
                               job->result.text_follow_data.available;
  }
  job->phase.store(BAG_ENCODE_JOB_PHASE_FINALIZING);
  UpdateMonotonicProgress(&job->progress_0_to_1, 1.0f);
  job->terminal_code.store(BAG_OK);
  job->state.store(BAG_ENCODE_JOB_SUCCEEDED);
}

void CompleteEncodeJobFailure(bag_encode_job* job, bag_error_code terminal_code,
                              bag_encode_job_state state) {
  ClearEncodeJobResult(job);
  job->terminal_code.store(terminal_code);
  job->state.store(state);
}

void JoinEncodeJobWorker(bag_encode_job* job) {
  if (job != nullptr && job->worker.joinable()) {
    job->worker.join();
  }
}

bag_error_code EnsureEncodeJobFollowResult(bag_encode_job* job) {
  if (job == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  const std::lock_guard lock(job->result_mutex);
  if (job->follow_result_ready) {
    return BAG_OK;
  }

  const bag_error_code follow_code =
      BuildEncodeFollowDataResult(job->core_config, job->text, &job->result);
  if (follow_code != BAG_OK) {
    return follow_code;
  }
  job->follow_result_ready = true;
  return BAG_OK;
}

void RunEncodeTextJobWorker(bag_encode_job* job) {
  if (job == nullptr) {
    return;
  }

  job->state.store(BAG_ENCODE_JOB_RUNNING);
  job->phase.store(BAG_ENCODE_JOB_PHASE_PREPARING_INPUT);
  job->progress_0_to_1.store(0.0f);
  job->terminal_code.store(BAG_NOT_READY);

  const bag::EncodeProgressSink progress_sink{
      .user_data = job,
      .on_progress = &OnEncodeJobProgress,
      .should_cancel = &ShouldCancelEncodeJob,
  };

  try {
    bag::EncodedPcmFollowResult result{};
    const bag::ErrorCode encode_code =
        bag::EncodeTextToPcm16(job->core_config, job->text, &result.pcm,
                               &progress_sink);
    switch (encode_code) {
      case bag::ErrorCode::kOk:
        CompleteEncodeJobSuccess(job, std::move(result));
        break;
      case bag::ErrorCode::kCancelled:
        CompleteEncodeJobFailure(job, BAG_CANCELLED, BAG_ENCODE_JOB_CANCELLED);
        break;
      default:
        CompleteEncodeJobFailure(job, ToApiCode(encode_code),
                                 BAG_ENCODE_JOB_FAILED);
        break;
    }
  } catch (const std::invalid_argument&) {
    CompleteEncodeJobFailure(job, BAG_INVALID_ARGUMENT, BAG_ENCODE_JOB_FAILED);
  } catch (...) {
    CompleteEncodeJobFailure(job, BAG_INTERNAL, BAG_ENCODE_JOB_FAILED);
  }
}
