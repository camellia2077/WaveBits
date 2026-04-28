cmake_minimum_required(VERSION 4.1.2)
project(audio_android_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
option(
    FLIPBITS_ANDROID_MODULES_SMOKE
    "Enable the opt-in Android named-modules/import-std smoke target."
    OFF
)
if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_compile_options(-O3 -DNDEBUG -ffunction-sections -fdata-sections)
    add_link_options(-Wl,--gc-sections)
endif()

get_filename_component(
    FLIPBITS_ROOT
    "${CMAKE_CURRENT_LIST_DIR}/../../../../../../"
    ABSOLUTE
)

if(FLIPBITS_ANDROID_MODULES_SMOKE)
    add_subdirectory(
        "${FLIPBITS_ROOT}/apps/audio_android/modules_smoke"
        "${CMAKE_CURRENT_BINARY_DIR}/deps/modules_smoke"
    )
endif()

add_subdirectory(
    "${FLIPBITS_ROOT}/apps/audio_android/native_package"
    "${CMAKE_CURRENT_BINARY_DIR}/deps/native_package"
)

add_library(audio_android_jni SHARED
    audio_io_jni.cpp
    jni_bridge.cpp
    jni_bridge_encode.cpp
    jni_bridge_helpers.cpp
    jni_bridge_lifecycle.cpp
    playback_runtime_jni.cpp
)

# Keep the app-side JNI target on the bag_api.h consumer boundary.
target_include_directories(audio_android_jni
    PRIVATE
        "${FLIPBITS_ROOT}/apps/audio_android/native_package/private_include"
        "${FLIPBITS_ROOT}/libs/audio_api/include"
        "${FLIPBITS_ROOT}/libs/audio_io/include"
        "${FLIPBITS_ROOT}/libs/audio_runtime/include"
)

target_link_libraries(audio_android_jni
    PRIVATE
        bag_android_native
        android
        log
)
