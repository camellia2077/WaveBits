#include "bag_api_object_defs_impl.inc"

namespace {
#include "bag_api_internal_decls_impl.inc"
#include "bag_api_config_mapping_impl.inc"
#include "bag_api_error_mapping_impl.inc"
#include "bag_api_job_progress_impl.inc"

// Keep marshalling and async job helpers in dedicated include units so this
// file can stay focused on ABI-facing config/error translation and entrypoints.
#include "bag_api_result_marshalling_impl.inc"
#include "bag_api_encode_job_support_impl.inc"
}  // namespace

#include "bag_api_mode_validation_entrypoints_impl.inc"
#include "bag_api_message_entrypoints_impl.inc"

// Split the outward ABI surface by operation family so future refactors can
// move encode and decode flows independently without re-mixing responsibilities.
#include "bag_api_encode_entrypoints_impl.inc"
#include "bag_api_decode_entrypoints_impl.inc"
