include("${WAVEBITS_ROOT}/cmake/wavebits_core_version.cmake")

set(WAVEBITS_ANDROID_NATIVE_PACKAGE_GENERATED_INCLUDE_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated/include")
file(MAKE_DIRECTORY "${WAVEBITS_ANDROID_NATIVE_PACKAGE_GENERATED_INCLUDE_DIR}/bag/common")
configure_file(
    "${WAVEBITS_ROOT}/libs/audio_core/include/bag/common/version_generated.h.in"
    "${WAVEBITS_ANDROID_NATIVE_PACKAGE_GENERATED_INCLUDE_DIR}/bag/common/version_generated.h"
    @ONLY
)

set(bag_android_native_package_source_files
    "${CMAKE_CURRENT_LIST_DIR}/bag_api_package.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_io_package.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_runtime_package.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_common_version.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_flash_codec.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_flash_signal.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_flash_voicing.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_flash_phy_clean.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_pro_codec.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_pro_phy_clean.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_transport_transport.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_ultra_codec.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_ultra_phy_clean.cpp"
)

if(WAVEBITS_ANDROID_MODULES_SMOKE)
    # Phase 3A leaf batch: let the direct version-owner target provide
    # bag.common.version so Android stops source-owning this leaf implementation.
    list(REMOVE_ITEM bag_android_native_package_source_files
        "${CMAKE_CURRENT_LIST_DIR}/audio_core_common_version.cpp"
    )
endif()

add_library(bag_android_native_package_objects OBJECT
    ${bag_android_native_package_source_files}
)

target_compile_features(bag_android_native_package_objects PUBLIC cxx_std_23)
set_target_properties(bag_android_native_package_objects PROPERTIES POSITION_INDEPENDENT_CODE ON)

if(WAVEBITS_ANDROID_MODULES_SMOKE)
    target_compile_definitions(bag_android_native_package_objects PRIVATE WAVEBITS_ANDROID_MODULES_SMOKE=1)
    target_link_libraries(bag_android_native_package_objects
        PRIVATE
            wavebits_android_bag_common_version_owner
    )
endif()

target_include_directories(bag_android_native_package_objects
    PUBLIC
        "${WAVEBITS_ROOT}/libs/audio_api/include"
        "${WAVEBITS_ROOT}/libs/audio_runtime/include"
    PRIVATE
        "${WAVEBITS_ROOT}/libs/audio_io/include"
        "${CMAKE_CURRENT_LIST_DIR}/../private_include"
        "${WAVEBITS_ANDROID_NATIVE_PACKAGE_GENERATED_INCLUDE_DIR}"
)
