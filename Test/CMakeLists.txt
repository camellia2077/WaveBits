wavebits_add_host_test(test_artifact
    NAME artifact_tests
    SOURCES artifact/artifact_tests.cpp
    LIBS bag_api audio_io
)

wavebits_add_host_test(test_cli_smoke
    NAME cli_smoke_tests
    SOURCES cli/cli_smoke_tests.cpp
    DEPENDS binary_audio_cpp_build
    COMMAND_ARGS $<TARGET_FILE:binary_audio_cpp>
)

wavebits_add_host_test(test_flash_voicing
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

wavebits_add_host_test(test_modules_foundation
    NAME modules_foundation_smoke
    SOURCES modules/foundation_module_smoke.cpp
    LIBS bag_core audio_io
    MODULE_TEST
)

wavebits_add_host_test(test_modules_leaf
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

wavebits_add_host_test(test_modules_mid_layer
    NAME modules_mid_layer_smoke
    SOURCES modules/mid_layer_module_smoke.cpp
    LIBS bag_core
    MODULE_TEST
)

wavebits_add_host_test(test_modules_facade_pipeline
    NAME modules_facade_pipeline_smoke
    SOURCES
        modules/facade_pipeline_smoke.cpp
        modules/facade_pipeline_smoke_support.cpp
        modules/facade_pipeline_smoke_transport.cpp
        modules/facade_pipeline_smoke_pipeline.cpp
    LIBS bag_core
    MODULE_TEST
)

wavebits_add_host_test(test_modules_end_to_end
    NAME modules_end_to_end_smoke
    SOURCES modules/end_to_end_module_smoke.cpp
    LIBS bag_core
    MODULE_TEST
)
