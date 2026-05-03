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
