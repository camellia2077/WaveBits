cmake_minimum_required(VERSION 3.28)

option(
    WAVEBITS_HOST_MODULES
    "Enable the default host-side named modules build path."
    ON
)

option(
    WAVEBITS_HOST_IMPORT_STD_PILOT
    "Enable the host-only import std; pilot for module-first host targets when the toolchain supports it."
    ON
)

if(WAVEBITS_HOST_MODULES AND WAVEBITS_HOST_IMPORT_STD_PILOT)
    set(CMAKE_EXPERIMENTAL_CXX_IMPORT_STD "d0edc3af-4c50-42ea-a356-e2862fe7a444")
endif()

project(BinaryAudioGeneratorCpp LANGUAGES CXX)

if(NOT WAVEBITS_HOST_MODULES)
    message(
        FATAL_ERROR
        "Root host WAVEBITS_HOST_MODULES=OFF has been retired. "
        "Use the default host modules build, and keep Android C++17 packaging isolated under "
        "apps/audio_android/native_package."
    )
endif()

if(POLICY CMP0155)
    cmake_policy(SET CMP0155 NEW)
endif()

if(NOT CMAKE_GENERATOR MATCHES "^(Ninja|Ninja Multi-Config|Visual Studio 17 2022)$")
    message(
        FATAL_ERROR
        "WAVEBITS_HOST_MODULES requires a generator with C++ modules scanning support."
    )
endif()

if(CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    if(CMAKE_CXX_COMPILER_VERSION VERSION_LESS 14)
        message(FATAL_ERROR "WAVEBITS_HOST_MODULES requires GCC 14 or newer.")
    endif()
elseif(CMAKE_CXX_COMPILER_ID STREQUAL "Clang")
    if(CMAKE_CXX_COMPILER_VERSION VERSION_LESS 16)
        message(FATAL_ERROR "WAVEBITS_HOST_MODULES requires Clang 16 or newer.")
    endif()
elseif(CMAKE_CXX_COMPILER_ID STREQUAL "MSVC")
    if(CMAKE_CXX_COMPILER_VERSION VERSION_LESS 19.34)
        message(FATAL_ERROR "WAVEBITS_HOST_MODULES requires MSVC 19.34 or newer.")
    endif()
else()
    message(
        FATAL_ERROR
        "WAVEBITS_HOST_MODULES supports GNU, Clang, or MSVC host compilers only."
    )
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_SCAN_FOR_MODULES ON)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(WAVEBITS_HOST_IMPORT_STD_PILOT_ENABLED OFF)
set(_wavebits_host_import_std_pilot_blocker "")
if(WAVEBITS_HOST_IMPORT_STD_PILOT)
    if(NOT 23 IN_LIST CMAKE_CXX_COMPILER_IMPORT_STD)
        set(_wavebits_host_import_std_pilot_blocker "active toolchain does not report C++23 import std support.")
    else()
        set(WAVEBITS_HOST_IMPORT_STD_PILOT_ENABLED ON)
    endif()
endif()

if(WAVEBITS_HOST_IMPORT_STD_PILOT AND NOT WAVEBITS_HOST_IMPORT_STD_PILOT_ENABLED)
    message(STATUS "Host import std pilot disabled: ${_wavebits_host_import_std_pilot_blocker}")
endif()

if(WAVEBITS_HOST_IMPORT_STD_PILOT_ENABLED)
    set(CMAKE_CXX_STANDARD 23)
endif()

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")

enable_testing()

add_subdirectory(libs/audio_core)
add_subdirectory(libs/audio_api)
add_subdirectory(libs/audio_io)
add_subdirectory(apps/audio_cli/windows/cmake)
add_subdirectory(Test)
