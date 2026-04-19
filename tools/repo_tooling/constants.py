from __future__ import annotations

from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_BUILD_DIR = ROOT_DIR / "build" / "dev"
DEFAULT_GENERATOR = "Ninja"
DEFAULT_CXX_COMPILER = "clang++"
ANDROID_GRADLE_ROOT = ROOT_DIR / "apps" / "audio_android"
ANDROID_APP_DIR = ANDROID_GRADLE_ROOT / "app"
DIST_DIR_NAME = "dist"
ANDROID_DIST_DIR = ROOT_DIR / DIST_DIR_NAME / "android"
CLI_TARGET_NAME = "binary_audio_cpp"
TEST_ARTIFACTS_DIR_NAME = "test-artifacts"
