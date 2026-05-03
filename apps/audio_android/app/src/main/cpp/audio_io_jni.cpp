#include <jni.h>

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

#include "audio_io_api.h"

namespace {

#include "audio_io_jni_strings_impl.inc"
#include "audio_io_jni_arrays_impl.inc"
#include "audio_io_jni_enum_lookup_impl.inc"
#include "audio_io_jni_dto_marshalling_impl.inc"

}  // namespace

#include "audio_io_jni_entrypoints_impl.inc"
