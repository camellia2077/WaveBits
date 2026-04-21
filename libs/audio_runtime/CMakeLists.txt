add_library(audio_runtime STATIC
    src/audio_runtime.cpp
)

target_compile_features(audio_runtime PUBLIC cxx_std_17)
target_include_directories(audio_runtime
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

if(BUILD_TESTING)
    flipbits_add_host_test(test_runtime
        NAME runtime_tests
        SOURCES tests/runtime_tests.cpp
        LIBS audio_runtime
    )
endif()
