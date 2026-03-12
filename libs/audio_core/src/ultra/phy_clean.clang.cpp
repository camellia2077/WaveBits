module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>
#endif

module bag.ultra.phy_clean;

import bag.ultra.codec;

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "phy_clean.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
