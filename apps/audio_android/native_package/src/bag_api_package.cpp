#include "bag_api.h"

#include <algorithm>
#include <atomic>
#include <iomanip>
#include <memory>
#include <mutex>
#include <new>
#include <sstream>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

#include "android_bag/transport/facade.h"
#include "android_bag/flash/codec.h"
#include "android_bag/flash/signal.h"

#if defined(FLIPBITS_ANDROID_MODULES_SMOKE)
import bag.common.version;
#else
#include "android_bag/common/version.h"
#endif

#include "../../../../libs/audio_api/src/bag_api_impl.inc"
