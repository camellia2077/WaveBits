#if !defined(WAVEBITS_MODULE_IMPL_WRAPPER)
#if __cplusplus >= 202002L
module;

module bag.common.version;
#else
#include "bag/legacy/common/version.h"
#endif
#endif

namespace bag {

const char* CoreVersion() {
    return "0.3.0";
}

}  // namespace bag
