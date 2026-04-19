module;

export module bag.common.types;

import std;

export import bag.common.config;

export namespace bag {

enum class EncodeProgressPhase {
  kPreparingInput = 0,
  kRenderingPcm = 1,
  kPostprocessing = 2,
  kFinalizing = 3,
};

using EncodeProgressCallback = void (*)(void* user_data,
                                        EncodeProgressPhase phase,
                                        float progress_0_to_1);
using EncodeShouldCancelCallback = bool (*)(void* user_data);

struct EncodeProgressSink {
  void* user_data = nullptr;
  EncodeProgressCallback on_progress = nullptr;
  EncodeShouldCancelCallback should_cancel = nullptr;
};

struct EncodeCancelled {};

inline bool ShouldCancelEncode(const EncodeProgressSink* sink) {
  return sink != nullptr && sink->should_cancel != nullptr &&
         sink->should_cancel(sink->user_data);
}

inline void ReportEncodeProgress(const EncodeProgressSink* sink,
                                 EncodeProgressPhase phase,
                                 float progress_0_to_1) {
  if (sink != nullptr && sink->on_progress != nullptr) {
    sink->on_progress(sink->user_data, phase, progress_0_to_1);
  }
}

struct PcmBlock {
  const std::int16_t* samples = nullptr;
  std::size_t sample_count = 0;
  std::int64_t timestamp_ms = 0;
};

struct IrPacket {
  std::vector<std::uint8_t> bits;
  std::int64_t timestamp_ms = 0;
  float confidence = 0.0f;
};

struct TextResult {
  std::string text;
  bool complete = false;
  float confidence = 0.0f;
  TransportMode mode = TransportMode::kFlash;
};

}  // namespace bag
