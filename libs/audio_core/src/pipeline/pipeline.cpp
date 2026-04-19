module;

import std;

module bag.pipeline;

import bag.transport.facade;

namespace bag {
namespace {

class PipelineAdapter final : public IPipeline {
 public:
  explicit PipelineAdapter(std::unique_ptr<ITransportDecoder> decoder)
      : decoder_(std::move(decoder)) {}

  ErrorCode PushPcm(const PcmBlock& block) override {
    if (decoder_ == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    return decoder_->PushPcm(block);
  }

  ErrorCode PollTextResult(TextResult* out_result) override {
    if (decoder_ == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    return decoder_->PollTextResult(out_result);
  }

  void Reset() override {
    if (decoder_ != nullptr) {
      decoder_->Reset();
    }
  }

 private:
  std::unique_ptr<ITransportDecoder> decoder_;
};

}  // namespace

std::unique_ptr<IPipeline> CreatePipeline(const CoreConfig& config) {
  auto decoder = CreateTransportDecoder(config);
  if (!decoder) {
    return nullptr;
  }
  return std::make_unique<PipelineAdapter>(std::move(decoder));
}

}  // namespace bag
