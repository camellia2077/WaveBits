from __future__ import annotations

import re
from datetime import date
from pathlib import Path

from .collect import (
    apply_scopes,
    collect_changed_paths,
    collect_version_hints,
    group_paths,
    normalize_scope,
    semantic_version_key,
)
from .model import BucketSummary, CandidateTopic, ChangedPath, HistoryPrepResult, RelevantSummary, VersionHint


def candidate_topic_for_bucket(bucket: str, items: list[ChangedPath]) -> CandidateTopic | None:
    paths = {item.path for item in items}

    if bucket == "android-app":
        if any(path.endswith("app/src/main/cpp/audio_io_jni.cpp") for path in paths):
            return CandidateTopic(
                title="android formal audio I/O boundary",
                reason="Detected Android JNI audio I/O changes that likely switch or reshape the app-facing WAV/metadata boundary.",
                bucket=bucket,
            )
        if any(path.endswith("data/NativePlaybackRuntimeGateway.kt") for path in paths) or any(
            "NativePlaybackRuntimeBridge" in path for path in paths
        ):
            return CandidateTopic(
                title="android playback runtime integration",
                reason="Detected Android playback runtime bridge/gateway changes for progress, scrub, or playback-state transitions.",
                bucket=bucket,
            )
        if any(path.endswith("app/build.gradle.kts") for path in paths) or any(path.endswith("gradle.properties") for path in paths):
            return CandidateTopic(
                title="android gradle root and release tooling",
                reason="Detected Android Gradle root, wrapper, or version-source changes under apps/audio_android.",
                bucket=bucket,
            )
        if any(path.endswith("domain/DecodedAudioData.kt") for path in paths):
            return CandidateTopic(
                title="android wav and metadata status split",
                reason="Detected Android decoded-audio domain changes that likely separate WAV status from metadata status.",
                bucket=bucket,
            )
        return CandidateTopic(
            title="android presentation workflow updates",
            reason="Detected Android app, domain, or native boundary changes under apps/audio_android.",
            bucket=bucket,
        )

    if bucket == "cli-app":
        if any(path.endswith("src/commands.rs") for path in paths) or any(path.endswith("src/main.rs") for path in paths):
            return CandidateTopic(
                title="rust cli command surface updates",
                reason="Detected Rust CLI command parsing or top-level command-surface changes.",
                bucket=bucket,
            )
        if any(path.endswith("src/bag_api.rs") for path in paths):
            return CandidateTopic(
                title="rust cli bag_api integration",
                reason="Detected Rust CLI bag_api FFI or encode/decode pipeline changes.",
                bucket=bucket,
            )
        if any(path.endswith("src/audio_io_api.rs") for path in paths):
            return CandidateTopic(
                title="rust cli wav and metadata integration",
                reason="Detected Rust CLI audio_io_api usage changes around WAV or metadata handling.",
                bucket=bucket,
            )
        if any(path.endswith("Cargo.toml") for path in paths) or any(path.endswith("Cargo.lock") for path in paths):
            return CandidateTopic(
                title="rust cli cargo and build workflow",
                reason="Detected Cargo manifest or Rust build workflow changes for the CLI presentation layer.",
                bucket=bucket,
            )
        return CandidateTopic(
            title="rust cli presentation updates",
            reason="Detected Rust CLI source or build changes under apps/audio_cli.",
            bucket=bucket,
        )

    if bucket == "libs/audio_io":
        if any(path.endswith("include/audio_io_api.h") for path in paths):
            return CandidateTopic(
                title="audio_io formal C ABI",
                reason="Detected `audio_io_api.h` changes under the audio I/O library boundary.",
                bucket=bucket,
            )
        return CandidateTopic(
            title="audio_io library contract updates",
            reason="Detected library-side audio I/O changes and/or tests.",
            bucket=bucket,
        )

    if bucket == "libs/audio_api":
        if any("/tests/" in path for path in paths):
            return CandidateTopic(
                title="bag_api contract and test coverage",
                reason="Detected `bag_api` boundary changes together with library-side tests.",
                bucket=bucket,
            )
        return CandidateTopic(
            title="bag_api boundary updates",
            reason="Detected public API and implementation changes in `libs/audio_api`.",
            bucket=bucket,
        )

    if bucket == "libs/audio_runtime":
        return CandidateTopic(
            title="audio_runtime state and test coverage",
            reason="Detected runtime library and runtime test changes.",
            bucket=bucket,
        )

    if bucket == "libs/audio_core":
        if any("voicing_internal_" in path for path in paths):
            return CandidateTopic(
                title="flash voicing internal split",
                reason="Detected `flash voicing` implementation split into internal support/texture/shell units.",
                bucket=bucket,
            )
        return CandidateTopic(
            title="audio_core module and implementation updates",
            reason="Detected core library source/module changes.",
            bucket=bucket,
        )

    if bucket == "tests":
        return CandidateTopic(
            title="cross-lib and product smoke regression coverage",
            reason="Detected root-level integration or smoke test changes.",
            bucket=bucket,
        )

    if bucket == "tools":
        return CandidateTopic(
            title="developer workflow tooling updates",
            reason="Detected `tools/run.py` or helper workflow changes.",
            bucket=bucket,
        )

    return None


def collect_candidate_topics(grouped_paths: dict[str, list[ChangedPath]]) -> list[CandidateTopic]:
    topics: list[CandidateTopic] = []
    for bucket, items in grouped_paths.items():
        topic = candidate_topic_for_bucket(bucket, items)
        if topic is not None:
            topics.append(topic)
    return topics


def pick_representative_files(bucket: str, items: list[ChangedPath]) -> list[str]:
    paths = [item.path for item in items]

    preferred_by_bucket: dict[str, list[str]] = {
        "android-app": [
            "apps/audio_android/app/src/main/cpp/audio_io_jni.cpp",
            "apps/audio_android/app/src/main/java/com/bag/audioandroid/domain/DecodedAudioData.kt",
            "apps/audio_android/app/src/main/java/com/bag/audioandroid/data/NativePlaybackRuntimeGateway.kt",
            "apps/audio_android/app/build.gradle.kts",
            "apps/audio_android/gradle.properties",
        ],
        "cli-app": [
            "apps/audio_cli/rust/src/commands.rs",
            "apps/audio_cli/rust/src/bag_api.rs",
            "apps/audio_cli/rust/src/audio_io_api.rs",
            "apps/audio_cli/rust/Cargo.toml",
            "apps/audio_cli/rust/src/lib.rs",
        ],
        "libs/audio_api": [
            "libs/audio_api/include/bag_api.h",
            "libs/audio_api/tests/api_tests.cpp",
        ],
        "libs/audio_core": [
            "libs/audio_core/src/flash/voicing_impl.inc",
        ],
        "libs/audio_io": [
            "libs/audio_io/include/audio_io_api.h",
            "libs/audio_io/include/wav_io.h",
            "libs/audio_io/tests/unit_tests.cpp",
        ],
        "libs/audio_runtime": [
            "libs/audio_runtime/include/audio_runtime.h",
            "libs/audio_runtime/tests/runtime_tests.cpp",
        ],
    }

    selected = [path for path in preferred_by_bucket.get(bucket, []) if path in paths]
    if selected:
        return selected
    return paths[:3]


def make_relevant_summary(grouped_paths: dict[str, list[ChangedPath]]) -> RelevantSummary:
    return RelevantSummary(
        bucket_counts={bucket: len(items) for bucket, items in grouped_paths.items()},
        changed_bucket_order=list(grouped_paths.keys()),
    )


def suggest_release_version(version_hints: list[VersionHint]) -> str:
    explicit_versions = [hint.value for hint in version_hints if hint.value.startswith("v")]
    if not explicit_versions:
        return "vX.Y.Z"
    return max(explicit_versions, key=semantic_version_key)


def version_from_target_history_file(target_history_file: Path | None) -> str | None:
    if target_history_file is None:
        return None
    stem = target_history_file.stem
    if re.fullmatch(r"\d+\.\d+\.\d+", stem):
        return f"v{stem}"
    return None


def build_history_prep_result(scopes: list[str], target_history_file: Path | None = None) -> HistoryPrepResult:
    # Centralize collection + summarization here so markdown/plain/json output
    # remains a pure rendering concern.
    normalized_scopes = [normalize_scope(scope) for scope in scopes]
    changed_paths = apply_scopes(collect_changed_paths(), normalized_scopes)
    grouped_paths = group_paths(changed_paths)
    version_hints = collect_version_hints(changed_paths)
    candidate_topics = collect_candidate_topics(grouped_paths)
    # Suggested date intentionally comes from the current working date because
    # history headings are written as "the day this draft is produced", not
    # inferred from git commit timestamps or existing docs.
    suggested_date = date.today().isoformat()
    # Suggested version prefers the explicit target history filename such as
    # docs/presentation/cli/v0.2/0.2.0.md -> v0.2.0. If no target file is
    # provided, fall back to repo history hints so the agent still gets a
    # version placeholder.
    suggested_version = version_from_target_history_file(target_history_file) or suggest_release_version(version_hints)
    relevant_summary = make_relevant_summary(grouped_paths)
    representative_files = [
        representative
        for bucket_name, items in grouped_paths.items()
        for representative in pick_representative_files(bucket_name, items)
    ]

    buckets: list[BucketSummary] = []
    for bucket_name, items in grouped_paths.items():
        bucket_topics = [topic for topic in candidate_topics if topic.bucket == bucket_name]
        buckets.append(
            BucketSummary(
                name=bucket_name,
                changed_files=items,
                candidate_topics=bucket_topics,
                representative_files=pick_representative_files(bucket_name, items),
                relevant_summary=RelevantSummary(
                    bucket_counts={bucket_name: len(items)},
                    changed_bucket_order=[bucket_name],
                ),
            )
        )

    return HistoryPrepResult(
        scopes=normalized_scopes,
        suggested_date=suggested_date,
        suggested_version=suggested_version,
        component_versions=version_hints,
        candidate_topics=candidate_topics,
        representative_files=representative_files,
        relevant_summary=relevant_summary,
        changed_files=changed_paths,
        buckets=buckets,
    )
