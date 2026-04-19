use std::env;
use std::path::PathBuf;

fn main() {
    println!("cargo:rerun-if-changed=build.rs");
    println!("cargo:rerun-if-changed=../../../libs/audio_api/include/bag_api.h");
    println!("cargo:rerun-if-changed=../../../libs/audio_io/include/audio_io_api.h");
    println!("cargo:rerun-if-env-changed=WAVEBITS_CMAKE_BUILD_DIR");

    if env::var("CARGO_CFG_WINDOWS").is_ok() && env::var("CARGO_CFG_TARGET_ENV").as_deref() != Ok("gnu") {
        panic!("WaveBits Rust CLI currently requires the x86_64-pc-windows-gnu target to link bag_api.");
    }

    let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").expect("CARGO_MANIFEST_DIR should be set"));
    let build_dir = env::var("WAVEBITS_CMAKE_BUILD_DIR")
        .map(PathBuf::from)
        .unwrap_or_else(|_| manifest_dir.join("..").join("..").join("..").join("build").join("dev"));
    let lib_dir = build_dir.join("lib");

    println!("cargo:rustc-link-search=native={}", lib_dir.display());
    println!("cargo:rustc-link-lib=static=bag_api");
    println!("cargo:rustc-link-lib=static=audio_io");
    println!("cargo:rustc-link-lib=static=bag_core");

    if env::var("CARGO_CFG_WINDOWS").is_ok() {
        println!("cargo:rustc-link-lib=dylib=stdc++");
    }
}
