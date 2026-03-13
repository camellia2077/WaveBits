#include "bag_api.h"

#include <algorithm>
#include <memory>
#include <new>
#include <stdexcept>
#include <string>
#include <vector>

#include "android_bag/common/version.h"
#include "android_bag/transport/facade.h"

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "../../../../libs/audio_api/src/bag_api.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
