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
