#include "audio_io_api.h"

#include <algorithm>
#include <cstring>
#include <new>
#include <string>
#include <vector>

#include "wav_io.h"

namespace {

#include "audio_io_api_status_mapping_impl.inc"
#include "audio_io_api_lifecycle_impl.inc"
#include "audio_io_api_metadata_marshalling_impl.inc"

}  // namespace

#include "audio_io_api_entrypoints_impl.inc"
#include "audio_io_api_status_messages_impl.inc"
#include "audio_io_api_free_impl.inc"
