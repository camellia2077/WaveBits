#pragma once

#include <vector>

#if __cplusplus >= 202002L
import bag.common.error_code;
import bag.common.types;
#else
#include "bag/legacy/common/error_code.h"
#include "bag/legacy/common/types.h"
#endif

namespace bag {

class ILinkLayer {
public:
    virtual ~ILinkLayer() = default;

    virtual ErrorCode PushIr(const IrPacket& packet) = 0;
    virtual ErrorCode PollPayload(std::vector<unsigned char>* out_payload) = 0;
    virtual void Reset() = 0;
};

}  // namespace bag
