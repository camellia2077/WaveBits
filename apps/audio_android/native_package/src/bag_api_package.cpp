#include "bag_api.h"

#include <algorithm>
#include <memory>
#include <new>
#include <stdexcept>
#include <string>
#include <vector>

#include "android_bag/transport/facade.h"

#if defined(WAVEBITS_ANDROID_MODULES_SMOKE)
import bag.common.version;
#else
#include "android_bag/common/version.h"
#endif

#include "../../../../libs/audio_api/src/bag_api_impl.inc"
