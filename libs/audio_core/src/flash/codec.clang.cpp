module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <cstdint>
#include <string>
#include <vector>
#endif

module bag.flash.codec;

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "codec.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
