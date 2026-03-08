add_library(bag_core STATIC
    src/common/version.cpp
    src/fsk/fsk_codec.cpp
    src/pro/frame_codec.cpp
    src/pro/text_codec.cpp
    src/pipeline/pipeline.cpp
)

target_compile_features(bag_core PUBLIC cxx_std_17)
target_include_directories(bag_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)
