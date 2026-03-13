#include "android_bag/flash/phy_clean.h"
#include "android_bag/pro/phy_clean.h"
#include "android_bag/transport/facade.h"
#include "android_bag/ultra/phy_clean.h"

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "../../../../libs/audio_core/src/transport/transport.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
