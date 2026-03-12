module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <cstddef>
#include <cstdint>
#include <vector>
#endif

module bag.transport.compat.frame_codec;

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "frame_codec.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
