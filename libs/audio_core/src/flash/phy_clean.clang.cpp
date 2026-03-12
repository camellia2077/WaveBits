module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>
#endif

module bag.flash.phy_clean;

import bag.flash.codec;

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "phy_clean.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
