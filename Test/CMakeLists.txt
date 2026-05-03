file(
    STRINGS
    "${PROJECT_SOURCE_DIR}/apps/audio_cli/rust/Cargo.toml"
    FLIPBITS_CLI_VERSION_LINE
    REGEX "^version = \"[0-9]+\\.[0-9]+\\.[0-9]+\"$"
)
if(NOT FLIPBITS_CLI_VERSION_LINE)
    message(FATAL_ERROR "Failed to read CLI presentation version from apps/audio_cli/rust/Cargo.toml.")
endif()
string(
    REGEX REPLACE
    "^version = \"([0-9]+\\.[0-9]+\\.[0-9]+)\"$"
    "\\1"
    FLIPBITS_CLI_PRESENTATION_VERSION
    "${FLIPBITS_CLI_VERSION_LINE}"
)

file(MAKE_DIRECTORY "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/test")
configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/include/test_cli_version_generated.h.in"
    "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/test/test_cli_version_generated.h"
    @ONLY
)

flipbits_add_host_test(test_artifact
    NAME artifact_tests
    SOURCES artifact/artifact_tests.cpp
    LIBS bag_api audio_io
)

flipbits_add_host_test(test_cli_smoke
    NAME cli_smoke_tests
    SOURCES cli/cli_smoke_tests.cpp
    DEPENDS FlipBits_build
    COMMAND_ARGS $<TARGET_FILE:FlipBits>
)

flipbits_add_host_test(test_flash_voicing
    NAME flash_voicing_tests
    SOURCES
        voicing/flash_voicing_tests.cpp
        voicing/flash_voicing_test_support.cpp
        voicing/flash_voicing_payload_tests.cpp
        voicing/flash_voicing_formal_tests.cpp
        voicing/flash_voicing_shell_tests.cpp
    LIBS bag_core
    MODULE_TEST
)

flipbits_add_host_test(test_modules_foundation
    NAME modules_foundation_smoke
    SOURCES modules/foundation_module_smoke.cpp
    LIBS bag_core audio_io
    MODULE_TEST
)

flipbits_add_host_test(test_modules_leaf
    NAME modules_leaf_smoke
    SOURCES
        modules/leaf_module_smoke.cpp
        modules/leaf_module_smoke_support.cpp
        modules/leaf_module_smoke_audio_io.cpp
        modules/leaf_module_smoke_flash.cpp
        modules/leaf_module_smoke_transport.cpp
    LIBS bag_core audio_io
    MODULE_TEST
)

flipbits_add_host_test(test_modules_mid_layer
    NAME modules_mid_layer_smoke
    SOURCES modules/mid_layer_module_smoke.cpp
    LIBS bag_core
    MODULE_TEST
)

flipbits_add_host_test(test_modules_facade_pipeline
    NAME modules_facade_pipeline_smoke
    SOURCES
        modules/facade_pipeline_smoke.cpp
        modules/facade_pipeline_smoke_support.cpp
        modules/facade_pipeline_smoke_transport.cpp
        modules/facade_pipeline_smoke_pipeline.cpp
    LIBS bag_core
    MODULE_TEST
)

flipbits_add_host_test(test_modules_end_to_end
    NAME modules_end_to_end_smoke
    SOURCES modules/end_to_end_module_smoke.cpp
    LIBS bag_core
    MODULE_TEST
)
