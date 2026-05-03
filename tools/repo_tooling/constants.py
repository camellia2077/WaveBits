from __future__ import annotations

from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
TOOLING_DIR = ROOT_DIR / "tooling"
BUILD_CONFIG_PATH = TOOLING_DIR / "build.toml"
DEFAULT_BUILD_DIR = ROOT_DIR / "build" / "dev"
DEFAULT_GENERATOR = "Ninja"
DEFAULT_CXX_COMPILER = "clang++"
ANDROID_GRADLE_ROOT = ROOT_DIR / "apps" / "audio_android"
ANDROID_APP_DIR = ANDROID_GRADLE_ROOT / "app"
CLI_RUST_DIR = ROOT_DIR / "apps" / "audio_cli" / "rust"
RUST_CLI_TARGET_TRIPLE = "x86_64-pc-windows-gnu"
RUST_CLI_WINDOWS_TOOLCHAIN = "stable-x86_64-pc-windows-gnu"
DIST_DIR_NAME = "dist"
ANDROID_DIST_DIR = ROOT_DIR / DIST_DIR_NAME / "android"
CLI_TARGET_NAME = "FlipBits"
TEST_ARTIFACTS_DIR_NAME = "test-artifacts"
