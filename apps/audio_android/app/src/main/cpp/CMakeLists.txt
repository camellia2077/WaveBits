cmake_minimum_required(VERSION 4.1.2)
project(audio_android_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
option(
    WAVEBITS_ANDROID_MODULES_SMOKE
    "Enable the opt-in Android named-modules/import-std smoke target."
    OFF
)

if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_compile_options(-O3 -DNDEBUG -ffunction-sections -fdata-sections)
    add_link_options(-Wl,--gc-sections)
endif()

get_filename_component(
    WAVEBITS_ROOT
    "${CMAKE_CURRENT_LIST_DIR}/../../../../../../"
    ABSOLUTE
)

if(WAVEBITS_ANDROID_MODULES_SMOKE)
    add_subdirectory(
        "${WAVEBITS_ROOT}/apps/audio_android/modules_smoke"
        "${CMAKE_CURRENT_BINARY_DIR}/deps/modules_smoke"
    )
endif()

add_subdirectory(
    "${WAVEBITS_ROOT}/apps/audio_android/native_package"
    "${CMAKE_CURRENT_BINARY_DIR}/deps/native_package"
)

add_library(audio_android_jni SHARED
    jni_bridge.cpp
)

# Keep the app-side JNI target on the bag_api.h consumer boundary.
target_include_directories(audio_android_jni
    PRIVATE
        "${WAVEBITS_ROOT}/libs/audio_api/include"
)

target_link_libraries(audio_android_jni
    PRIVATE
        bag_android_native
        android
        log
)
