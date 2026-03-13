set(bag_android_native_wrapper_sources
    "${CMAKE_CURRENT_LIST_DIR}/bag_api_package.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_common_version.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_flash_codec.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_flash_phy_clean.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_pro_codec.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_pro_phy_clean.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_transport_transport.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_ultra_codec.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/audio_core_ultra_phy_clean.cpp"
)

add_library(bag_android_native_wrappers OBJECT
    ${bag_android_native_wrapper_sources}
)

target_compile_features(bag_android_native_wrappers PUBLIC cxx_std_17)
set_target_properties(bag_android_native_wrappers PROPERTIES POSITION_INDEPENDENT_CODE ON)

target_include_directories(bag_android_native_wrappers
    PUBLIC
        "${WAVEBITS_ROOT}/libs/audio_api/include"
    PRIVATE
        "${CMAKE_CURRENT_LIST_DIR}/../private_include"
)
