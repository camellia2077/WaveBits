#pragma once

#include "android_bag/common/error_code.h"
#include "android_bag/common/types.h"

namespace bag {

class ITransportDecoder {
public:
    virtual ~ITransportDecoder() = default;

    virtual ErrorCode PushPcm(const PcmBlock& block) = 0;
    virtual ErrorCode PollTextResult(TextResult* out_result) = 0;
    virtual void Reset() = 0;
};

}  // namespace bag
