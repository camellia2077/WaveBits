#include <algorithm>
#include <cmath>
#include <stdexcept>

#include "android_bag/flash/codec.h"
#include "android_bag/flash/phy_clean.h"

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "../../../../libs/audio_core/src/flash/phy_clean.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
