#include <algorithm>
#include <cmath>

#include "android_bag/ultra/codec.h"
#include "android_bag/ultra/phy_clean.h"

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "../../../../libs/audio_core/src/ultra/phy_clean.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
