cmake_minimum_required(VERSION 3.20)

project(BinaryAudioGeneratorCpp LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")

enable_testing()

add_subdirectory(libs/audio_core)
add_subdirectory(libs/audio_api)
add_subdirectory(libs/audio_io)
add_subdirectory(apps/audio_cli/windows/cmake)
add_subdirectory(Test)
