set(bag_core_sources)

file(MAKE_DIRECTORY "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common")
configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/include/bag/common/version_generated.h.in"
    "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common/version_generated.h"
    @ONLY
)

set(bag_core_sources
    src/common/version.cpp
    src/flash/codec.cpp
    src/flash/phy_clean.cpp
    src/flash/signal.cpp
    src/flash/voicing.cpp
    src/fsk/fsk_codec.cpp
    src/pro/codec.cpp
    src/pro/phy_clean.cpp
    src/pro/phy_compat.cpp
    src/transport/compat/frame_codec.cpp
    src/transport/transport.cpp
    src/ultra/codec.cpp
    src/ultra/phy_clean.cpp
    src/ultra/phy_compat.cpp
    src/pipeline/pipeline.cpp
)

add_library(bag_core STATIC
    ${bag_core_sources}
)

target_compile_features(bag_core PUBLIC cxx_std_20)
target_include_directories(bag_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}
)

target_sources(
    bag_core
    PUBLIC
        FILE_SET cxx_modules TYPE CXX_MODULES
        BASE_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/modules
        FILES
            modules/bag/common/config.cppm
            modules/bag/common/error_code.cppm
            modules/bag/common/types.cppm
            modules/bag/common/version.cppm
            modules/bag/flash/codec.cppm
            modules/bag/flash/phy_clean.cppm
            modules/bag/flash/signal.cppm
            modules/bag/flash/voicing.cppm
            modules/bag/fsk/codec.cppm
            modules/bag/pro/codec.cppm
            modules/bag/pro/phy_clean.cppm
            modules/bag/pro/phy_compat.cppm
            modules/bag/transport/decoder.cppm
            modules/bag/transport/compat/frame_codec.cppm
            modules/bag/transport/facade.cppm
            modules/bag/ultra/codec.cppm
            modules/bag/ultra/phy_clean.cppm
            modules/bag/ultra/phy_compat.cppm
            modules/bag/pipeline/pipeline.cppm
)

target_compile_features(bag_core PRIVATE cxx_std_23)
set_property(TARGET bag_core PROPERTY CXX_MODULE_STD ON)
