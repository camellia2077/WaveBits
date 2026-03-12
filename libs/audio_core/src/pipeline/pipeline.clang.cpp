module;

#if defined(WAVEBITS_CORE_IMPORT_STD)
import std;
#else
#include <memory>
#include <utility>
#endif

module bag.pipeline;

import bag.transport.facade;

#define WAVEBITS_MODULE_IMPL_WRAPPER 1
#include "pipeline.cpp"
#undef WAVEBITS_MODULE_IMPL_WRAPPER
