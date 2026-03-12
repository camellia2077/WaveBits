#pragma once

#include <vector>

#if __cplusplus >= 202002L
import bag.common.error_code;
import bag.common.types;
#else
#include "bag/legacy/common/error_code.h"
#include "bag/legacy/common/types.h"
#endif

namespace bag::fun {

class IFunPhy {
public:
    virtual ~IFunPhy() = default;

    virtual ErrorCode Modulate(const std::vector<unsigned char>& payload,
                               std::vector<float>* out_pcm) = 0;
    virtual ErrorCode Demodulate(const PcmBlock& block, IrPacket* out_packet) = 0;
    virtual void Reset() = 0;
};

}  // namespace bag::fun
