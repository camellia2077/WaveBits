add_library(bag_core STATIC
    src/common/version.cpp
    src/flash/codec.cpp
    src/flash/phy_clean.cpp
    src/fsk/fsk_codec.cpp
    src/pro/codec.cpp
    src/pro/text_codec.cpp
    src/pro/frame_codec.cpp
    src/pro/phy_clean.cpp
    src/pro/phy_compat.cpp
    src/transport/compat/frame_codec.cpp
    src/transport/transport.cpp
    src/ultra/codec.cpp
    src/ultra/phy_clean.cpp
    src/ultra/phy_compat.cpp
    src/pipeline/pipeline.cpp
)

target_compile_features(bag_core PUBLIC cxx_std_17)
target_include_directories(bag_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)
