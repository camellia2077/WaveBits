module;

#if !defined(WAVEBITS_CORE_IMPORT_STD)
#include <memory>
#endif

export module bag.pipeline;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#endif

export import bag.common.config;
export import bag.common.error_code;
export import bag.common.types;

export namespace bag {

class IPipeline {
public:
    virtual ~IPipeline() = default;

    virtual ErrorCode PushPcm(const PcmBlock& block) = 0;
    virtual ErrorCode PollTextResult(TextResult* out_result) = 0;
    virtual void Reset() = 0;
};

std::unique_ptr<IPipeline> CreatePipeline(const CoreConfig& config);

}  // namespace bag
