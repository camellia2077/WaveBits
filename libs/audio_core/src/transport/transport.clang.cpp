module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <cstdint>
#include <memory>
#include <string>
#include <string_view>
#include <vector>
#endif

module bag.transport.facade;

import bag.flash.phy_clean;
import bag.pro.phy_clean;
import bag.ultra.phy_clean;

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "transport.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
