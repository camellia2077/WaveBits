module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <cstdint>
#include <string>
#include <vector>
#endif

module bag.fsk.codec;

import bag.flash.phy_clean;

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "fsk_codec.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
