cmake_minimum_required(VERSION 3.28)

set(CMAKE_EXPERIMENTAL_CXX_IMPORT_STD "d0edc3af-4c50-42ea-a356-e2862fe7a444")

project(BinaryAudioGeneratorCpp LANGUAGES CXX)

include("${CMAKE_CURRENT_SOURCE_DIR}/cmake/wavebits_core_version.cmake")
set(WAVEBITS_CORE_GENERATED_INCLUDE_DIR "${PROJECT_BINARY_DIR}/generated/include")

if(POLICY CMP0155)
    cmake_policy(SET CMP0155 NEW)
endif()

if(NOT CMAKE_GENERATOR STREQUAL "Ninja")
    message(
        FATAL_ERROR
        "Root host currently supports the single clang++ + Ninja lane only."
    )
endif()

if(NOT CMAKE_CXX_COMPILER_ID STREQUAL "Clang")
    message(
        FATAL_ERROR
        "Root host currently supports clang++ only. "
        "GNU/MSVC/Visual Studio host lanes are outside the current supported promotion target."
    )
endif()

if(CMAKE_CXX_COMPILER_VERSION VERSION_LESS 16)
    message(FATAL_ERROR "Root host currently requires Clang 16 or newer.")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_SCAN_FOR_MODULES ON)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT 23 IN_LIST CMAKE_CXX_COMPILER_IMPORT_STD)
    message(
        FATAL_ERROR
        "Current root host target requires C++23 import std support from the active Clang toolchain."
    )
endif()

set(CMAKE_CXX_STANDARD 23)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")

enable_testing()

add_subdirectory(libs/audio_core)
add_subdirectory(libs/audio_api)
add_subdirectory(libs/audio_io)
add_subdirectory(apps/audio_cli/windows/cmake)
add_subdirectory(Test)
