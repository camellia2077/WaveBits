find_program(CARGO_EXECUTABLE cargo REQUIRED)

set(WAVEBITS_RUST_CLI_DIR "${CMAKE_CURRENT_LIST_DIR}/..")
set(WAVEBITS_RUST_CLI_MANIFEST "${WAVEBITS_RUST_CLI_DIR}/Cargo.toml")
set(WAVEBITS_RUST_TARGET_TRIPLE "x86_64-pc-windows-gnu")

if(WIN32)
    set(WAVEBITS_RUST_CLI_BINARY
        "${WAVEBITS_RUST_CLI_DIR}/target/${WAVEBITS_RUST_TARGET_TRIPLE}/debug/binary_audio_cpp.exe")
else()
    set(WAVEBITS_RUST_CLI_BINARY
        "${WAVEBITS_RUST_CLI_DIR}/target/${WAVEBITS_RUST_TARGET_TRIPLE}/debug/binary_audio_cpp")
endif()

add_custom_target(binary_audio_cpp_build
    DEPENDS
        wavebits_rust_cli_native_deps
    COMMAND
        "${CMAKE_COMMAND}" -E env
        "WAVEBITS_CMAKE_BUILD_DIR=${CMAKE_BINARY_DIR}"
        "${CARGO_EXECUTABLE}"
        build
        --manifest-path "${WAVEBITS_RUST_CLI_MANIFEST}"
        --target "${WAVEBITS_RUST_TARGET_TRIPLE}"
    WORKING_DIRECTORY "${WAVEBITS_RUST_CLI_DIR}"
    COMMENT "Building Rust CLI via Cargo"
    VERBATIM
)

add_executable(binary_audio_cpp IMPORTED GLOBAL)
set_target_properties(binary_audio_cpp PROPERTIES
    IMPORTED_LOCATION "${WAVEBITS_RUST_CLI_BINARY}"
)
