from __future__ import annotations

from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_BUILD_DIR = ROOT_DIR / "build" / "dev"
DEFAULT_GENERATOR = "Ninja"
ANDROID_DIR = ROOT_DIR / "apps" / "audio_android"
CLI_TARGET_NAME = "binary_audio_cpp"
TEST_ARTIFACTS_DIR_NAME = "test-artifacts"
