# Android native packaging target that keeps the app-side CMake detached
# from the repo's host-oriented library targets while preserving the
# bag_api.h consumer boundary. The package compiles only Android-private
# package-owned implementation sources, so it no longer directly owns raw
# repo implementation sources.
if(NOT DEFINED WAVEBITS_ROOT)
    get_filename_component(
        WAVEBITS_ROOT
        "${CMAKE_CURRENT_LIST_DIR}/../../.."
        ABSOLUTE
    )
endif()

add_subdirectory(
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${CMAKE_CURRENT_BINARY_DIR}/src"
)

# Keep the package entry point detached from the Android-private package
# source list. The implementation ownership lives in src/CMakeLists.txt.
add_library(bag_android_native_packaged STATIC
    $<TARGET_OBJECTS:bag_android_native_package_objects>
)
add_library(bag_android_native ALIAS bag_android_native_packaged)

target_compile_features(bag_android_native_packaged PUBLIC cxx_std_23)
set_target_properties(bag_android_native_packaged PROPERTIES POSITION_INDEPENDENT_CODE ON)

if(WAVEBITS_ANDROID_MODULES_SMOKE)
    target_link_libraries(bag_android_native_packaged
        PUBLIC
            wavebits_android_bag_common_version_owner
            wavebits_android_modules_smoke
    )
endif()

target_include_directories(bag_android_native_packaged
    PUBLIC
        "${WAVEBITS_ROOT}/libs/audio_api/include"
)
