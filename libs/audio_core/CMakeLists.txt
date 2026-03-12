set(bag_core_sources
    src/common/version.cpp
    src/flash/codec.cpp
    src/flash/phy_clean.cpp
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

if(WAVEBITS_HOST_MODULES)
    list(APPEND bag_core_sources
        src/fsk/fsk_codec.cpp
    )
endif()

if(WAVEBITS_HOST_MODULES AND CMAKE_CXX_COMPILER_ID STREQUAL "Clang")
    set(bag_core_sources
        src/common/version.clang.cpp
        src/flash/codec.clang.cpp
        src/flash/phy_clean.clang.cpp
        src/fsk/fsk_codec.clang.cpp
        src/pro/codec.clang.cpp
        src/pro/phy_clean.clang.cpp
        src/pro/phy_compat.clang.cpp
        src/transport/compat/frame_codec.clang.cpp
        src/transport/transport.clang.cpp
        src/ultra/codec.clang.cpp
        src/ultra/phy_clean.clang.cpp
        src/ultra/phy_compat.clang.cpp
        src/pipeline/pipeline.clang.cpp
    )
endif()

add_library(bag_core STATIC
    ${bag_core_sources}
)

target_compile_features(bag_core PUBLIC cxx_std_17)
target_include_directories(bag_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

if(WAVEBITS_HOST_MODULES)
    target_compile_features(bag_core PUBLIC cxx_std_20)
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

    if(WAVEBITS_HOST_IMPORT_STD_PILOT_ENABLED)
        target_compile_features(bag_core PRIVATE cxx_std_23)
        target_compile_definitions(bag_core PRIVATE WAVEBITS_CORE_IMPORT_STD=1)
        set_property(TARGET bag_core PROPERTY CXX_MODULE_STD ON)
    endif()
endif()
