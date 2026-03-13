#pragma once

#include <vector>

#if __cplusplus >= 202002L
import bag.common.error_code;
import bag.common.types;
#else
#include "bag/interface/common/error_code.h"
#include "bag/interface/common/types.h"
#endif

namespace bag::pro {

class IProPhy {
public:
    virtual ~IProPhy() = default;

    virtual ErrorCode Modulate(const std::vector<unsigned char>& payload,
                               std::vector<float>* out_pcm) = 0;
    virtual ErrorCode Demodulate(const PcmBlock& block, IrPacket* out_packet) = 0;
    virtual ErrorCode EstimateQuality(float* out_snr_db, float* out_ber) = 0;
    virtual void Reset() = 0;
};

}  // namespace bag::pro
