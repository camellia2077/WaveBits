include("${WAVEBITS_ROOT}/cmake/wavebits_core_version.cmake")

set(CMAKE_CXX_SCAN_FOR_MODULES ON)

if(NOT DEFINED WAVEBITS_CORE_GENERATED_INCLUDE_DIR)
    set(
        WAVEBITS_ANDROID_MODULES_SMOKE_CORE_GENERATED_INCLUDE_DIR
        "${CMAKE_CURRENT_BINARY_DIR}/generated/include"
    )
    set(
        WAVEBITS_CORE_GENERATED_INCLUDE_DIR
        "${WAVEBITS_ANDROID_MODULES_SMOKE_CORE_GENERATED_INCLUDE_DIR}"
    )
endif()

file(MAKE_DIRECTORY "${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common")
configure_file(
    "${WAVEBITS_ROOT}/libs/audio_core/include/bag/common/version_generated.h.in"
    "${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common/version_generated.h"
    @ONLY
)

set(WAVEBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_LEVELS "unavailable")
if(DEFINED CMAKE_CXX_COMPILER_IMPORT_STD AND NOT CMAKE_CXX_COMPILER_IMPORT_STD STREQUAL "")
    string(
        REPLACE
        ";"
        ", "
        WAVEBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_LEVELS
        "${CMAKE_CXX_COMPILER_IMPORT_STD}"
    )
endif()

set(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY "unavailable")
set(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_EXISTS "0")
set(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN "missing")
execute_process(
    COMMAND
        "${CMAKE_CXX_COMPILER}"
        ${CMAKE_CXX_COMPILER_ID_ARG1}
        "-print-file-name=libc++.modules.json"
    OUTPUT_VARIABLE WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY
    ERROR_QUIET
    RESULT_VARIABLE WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_RESULT
    OUTPUT_STRIP_TRAILING_WHITESPACE
)
if(
    WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_RESULT EQUAL 0
    AND EXISTS "${WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY}"
)
    set(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_EXISTS "1")
    file(
        TO_CMAKE_PATH
        "${WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY}"
        WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY_NORMALIZED
    )
    file(TO_CMAKE_PATH "${CMAKE_ANDROID_NDK}" WAVEBITS_ANDROID_MODULES_SMOKE_ANDROID_NDK_NORMALIZED)
    string(
        FIND
        "${WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY_NORMALIZED}"
        "${WAVEBITS_ANDROID_MODULES_SMOKE_ANDROID_NDK_NORMALIZED}/"
        WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_UNDER_NDK
    )
    if(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_UNDER_NDK EQUAL 0)
        set(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN "android-ndk-local")
    else()
        set(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN "foreign")
    endif()
endif()

set(
    WAVEBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_STATUS
    "blocked-missing-libcxx-modules-json"
)
if(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN STREQUAL "android-ndk-local")
    set(WAVEBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_STATUS "candidate-available")
elseif(WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN STREQUAL "foreign")
    set(WAVEBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_STATUS "blocked-foreign-libcxx-modules-json")
endif()

set(WAVEBITS_ANDROID_MODULES_SMOKE_NDK_VERSION "unknown")
if(DEFINED CMAKE_ANDROID_NDK_VERSION)
    set(WAVEBITS_ANDROID_MODULES_SMOKE_NDK_VERSION "${CMAKE_ANDROID_NDK_VERSION}")
endif()

set(WAVEBITS_ANDROID_MODULES_SMOKE_PLATFORM "unknown")
if(DEFINED ANDROID_PLATFORM)
    set(WAVEBITS_ANDROID_MODULES_SMOKE_PLATFORM "${ANDROID_PLATFORM}")
endif()

set(WAVEBITS_ANDROID_MODULES_SMOKE_API "unknown")
if(DEFINED CMAKE_ANDROID_API)
    set(WAVEBITS_ANDROID_MODULES_SMOKE_API "${CMAKE_ANDROID_API}")
elseif(DEFINED ANDROID_NATIVE_API_LEVEL)
    set(WAVEBITS_ANDROID_MODULES_SMOKE_API "${ANDROID_NATIVE_API_LEVEL}")
endif()

set(
    WAVEBITS_ANDROID_MODULES_SMOKE_REPORT_PATH
    "${CMAKE_CURRENT_BINARY_DIR}/android-modules-smoke-report.txt"
)
file(
    WRITE
    "${WAVEBITS_ANDROID_MODULES_SMOKE_REPORT_PATH}"
    "wavebits_core_version=${WAVEBITS_CORE_VERSION}\n"
    "cmake_version=${CMAKE_VERSION}\n"
    "generator=${CMAKE_GENERATOR}\n"
    "system_name=${CMAKE_SYSTEM_NAME}\n"
    "android_abi=${CMAKE_ANDROID_ARCH_ABI}\n"
    "android_platform=${WAVEBITS_ANDROID_MODULES_SMOKE_PLATFORM}\n"
    "android_api=${WAVEBITS_ANDROID_MODULES_SMOKE_API}\n"
    "cxx_compiler_id=${CMAKE_CXX_COMPILER_ID}\n"
    "cxx_compiler_version=${CMAKE_CXX_COMPILER_VERSION}\n"
    "android_ndk=${CMAKE_ANDROID_NDK}\n"
    "android_ndk_version=${WAVEBITS_ANDROID_MODULES_SMOKE_NDK_VERSION}\n"
    "cxx_import_std=${WAVEBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_LEVELS}\n"
    "libcxx_modules_json_query=${WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY}\n"
    "libcxx_modules_json_exists=${WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_EXISTS}\n"
    "libcxx_modules_json_origin=${WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN}\n"
    "import_std_status=${WAVEBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_STATUS}\n"
    "mode=phase3a-direct-module-version-owner\n"
)

message(STATUS "WaveBits Android modules smoke enabled")
message(STATUS "  ABI: ${CMAKE_ANDROID_ARCH_ABI}")
message(STATUS "  compiler: ${CMAKE_CXX_COMPILER_ID} ${CMAKE_CXX_COMPILER_VERSION}")
message(STATUS "  import std levels: ${WAVEBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_LEVELS}")
message(
    STATUS
    "  libc++.modules.json origin: ${WAVEBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN}"
)
message(STATUS "  report: ${WAVEBITS_ANDROID_MODULES_SMOKE_REPORT_PATH}")

add_library(wavebits_android_bag_common_version_owner STATIC
    "${WAVEBITS_ROOT}/libs/audio_core/src/common/version.cpp"
)

target_compile_features(wavebits_android_bag_common_version_owner PUBLIC cxx_std_23)
set_target_properties(
    wavebits_android_bag_common_version_owner
    PROPERTIES POSITION_INDEPENDENT_CODE ON
)

target_include_directories(wavebits_android_bag_common_version_owner
    PUBLIC
        "${WAVEBITS_ROOT}/libs/audio_core/include"
        "${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}"
)

target_sources(
    wavebits_android_bag_common_version_owner
    PUBLIC
        FILE_SET cxx_modules TYPE CXX_MODULES
        BASE_DIRS "${WAVEBITS_ROOT}/libs/audio_core/modules"
        FILES "${WAVEBITS_ROOT}/libs/audio_core/modules/bag/common/version.cppm"
)

add_library(wavebits_android_modules_smoke STATIC
    "${CMAKE_CURRENT_LIST_DIR}/android_modules_smoke.cpp"
)

target_compile_features(wavebits_android_modules_smoke PRIVATE cxx_std_23)
set_target_properties(wavebits_android_modules_smoke PROPERTIES POSITION_INDEPENDENT_CODE ON)

target_link_libraries(wavebits_android_modules_smoke
    PRIVATE
        wavebits_android_bag_common_version_owner
)
