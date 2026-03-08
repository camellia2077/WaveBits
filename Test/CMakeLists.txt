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
        bag_core
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
