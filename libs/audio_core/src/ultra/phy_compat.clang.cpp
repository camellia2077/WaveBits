module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <cstdint>
#include <memory>
#include <string>
#include <vector>
#endif

module bag.ultra.phy_compat;

import bag.flash.phy_clean;
import bag.transport.compat.frame_codec;
import bag.ultra.codec;

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "phy_compat.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
