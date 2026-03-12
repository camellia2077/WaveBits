add_executable(test_unit
    unit/unit_tests.cpp
)
target_include_directories(test_unit
    PRIVATE
        include
)
target_link_libraries(test_unit
    PRIVATE
        bag_core
        audio_io
)
add_test(NAME unit_tests COMMAND test_unit)

add_executable(test_api
    api/api_tests.cpp
)
target_include_directories(test_api
    PRIVATE
        include
)
target_link_libraries(test_api
    PRIVATE
        bag_api
)
add_test(NAME api_tests COMMAND test_api)

add_executable(test_artifact
    artifact/artifact_tests.cpp
)
target_include_directories(test_artifact
    PRIVATE
        include
)
target_link_libraries(test_artifact
    PRIVATE
        bag_api
        audio_io
)
add_test(NAME artifact_tests COMMAND test_artifact)

add_executable(test_cli_smoke
    cli/cli_smoke_tests.cpp
)
target_include_directories(test_cli_smoke
    PRIVATE
        include
)
add_dependencies(test_cli_smoke binary_audio_cpp)
add_test(
    NAME cli_smoke_tests
    COMMAND test_cli_smoke $<TARGET_FILE:binary_audio_cpp>
)

if(WAVEBITS_HOST_MODULES)
    function(wavebits_configure_module_test target_name)
        target_include_directories(${target_name}
            PRIVATE
                include
        )
        target_compile_features(${target_name} PRIVATE cxx_std_20)
        if(WAVEBITS_HOST_IMPORT_STD_PILOT_ENABLED)
            target_compile_features(${target_name} PRIVATE cxx_std_23)
            target_compile_definitions(${target_name} PRIVATE WAVEBITS_TEST_IMPORT_STD=1)
            set_property(TARGET ${target_name} PROPERTY CXX_MODULE_STD ON)
        endif()
    endfunction()

    wavebits_configure_module_test(test_unit)

    add_executable(test_modules_phase0
        modules/phase0_smoke.cpp
    )
    target_link_libraries(test_modules_phase0
        PRIVATE
            bag_core
            audio_io
    )
    wavebits_configure_module_test(test_modules_phase0)
    add_test(NAME modules_phase0_smoke COMMAND test_modules_phase0)

    add_executable(test_modules_phase2
        modules/phase2_leaf_smoke.cpp
    )
    target_link_libraries(test_modules_phase2
        PRIVATE
            bag_core
            audio_io
    )
    wavebits_configure_module_test(test_modules_phase2)
    add_test(NAME modules_phase2_leaf_smoke COMMAND test_modules_phase2)

    add_executable(test_modules_phase3
        modules/phase3_mid_layer_smoke.cpp
    )
    target_link_libraries(test_modules_phase3
        PRIVATE
            bag_core
    )
    wavebits_configure_module_test(test_modules_phase3)
    add_test(NAME modules_phase3_mid_layer_smoke COMMAND test_modules_phase3)

    add_executable(test_modules_phase4
        modules/phase4_facade_pipeline_smoke.cpp
    )
    target_link_libraries(test_modules_phase4
        PRIVATE
            bag_core
    )
    wavebits_configure_module_test(test_modules_phase4)
    add_test(NAME modules_phase4_facade_pipeline_smoke COMMAND test_modules_phase4)

    add_executable(test_modules_phase10
        modules/phase10_internal_cutover.cpp
    )
    target_link_libraries(test_modules_phase10
        PRIVATE
            bag_core
    )
    wavebits_configure_module_test(test_modules_phase10)
    add_test(NAME modules_phase10_internal_cutover COMMAND test_modules_phase10)
endif()
