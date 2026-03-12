#pragma once

#include <memory>

#include "bag/legacy/common/config.h"
#include "bag/legacy/common/error_code.h"
#include "bag/legacy/common/types.h"

namespace bag {

class IPipeline {
public:
    virtual ~IPipeline() = default;

    virtual ErrorCode PushPcm(const PcmBlock& block) = 0;
    virtual ErrorCode PollTextResult(TextResult* out_result) = 0;
    virtual void Reset() = 0;
};

std::unique_ptr<IPipeline> CreatePipeline(const CoreConfig& config);

}  // namespace bag
