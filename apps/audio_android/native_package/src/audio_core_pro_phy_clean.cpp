#include <algorithm>
#include <cmath>

#include "android_bag/pro/codec.h"
#include "android_bag/pro/phy_clean.h"

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "../../../../libs/audio_core/src/pro/phy_clean.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
