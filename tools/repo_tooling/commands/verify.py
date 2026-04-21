from __future__ import annotations

import argparse
import os
from pathlib import Path
from typing import Callable

from .android import cmd_android
from .audio_io_boundary_policy import run_audio_io_boundary_policy_checks
from .boundary_policy import run_boundary_policy_checks
from .build import cmd_build
from .configure import cmd_configure
from .format import cmd_format
from .module_structure_policy import run_module_structure_policy_checks
from .retirement_policy import run_retirement_policy_checks
from .test import cmd_test
from ..paths import resolve_build_dir
from ..process import run

RUST_CLI_DIR = Path("apps/audio_cli/rust")
RUST_TARGET_TRIPLE = "x86_64-pc-windows-gnu"

VERIFY_CHECK_GROUPS: tuple[tuple[str, str, tuple[str, ...]], ...] = (
    (
        "module_structure",
        "Guard current named-module implementation units and module-first wiring.",
        ("named_module_tus", "module_first_wiring"),
    ),
    (
        "boundary",
        "Guard stable consumer surfaces and keep boundary-first/module-first tests separated.",
        ("consumer_surfaces", "boundary_first_tests", "module_first_tests"),
    ),
    (
        "audio_io_boundary",
        "Guard the permanent audio_io boundary model, private backend split, and sndfile containment.",
        ("stable_wav_boundary", "private_backend_split", "third_party_containment"),
    ),
    (
        "retirement",
        "Guard boundary-adjacent host wiring, retired wrappers, Android private header self-containment, and post-legacy deleted surfaces.",
        ("boundary_hosts", "retired_wrappers", "android_private_headers", "post_legacy_surfaces"),
    ),
)

VERIFY_STATIC_CHECK_RUNNERS: tuple[tuple[str, Callable[[], None]], ...] = (
    ("module_structure", run_module_structure_policy_checks),
    ("boundary", run_boundary_policy_checks),
    ("audio_io_boundary", run_audio_io_boundary_policy_checks),
    ("retirement", run_retirement_policy_checks),
)

def _print_verify_banner(message: str) -> None:
    print(f"\n[verify] {message}", flush=True)


def format_verify_check_groups() -> str:
    lines = ["Current static check groups run before configure/build/test:"]
    for name, description, subchecks in VERIFY_CHECK_GROUPS:
        lines.append(f"- {name}: {description}")
        lines.append(f"  Includes: {', '.join(subchecks)}")
    return "\n".join(lines)


def run_verify_static_checks() -> None:
    _print_verify_banner("Step 1/4: running static policy checks")
    for name, runner in VERIFY_STATIC_CHECK_RUNNERS:
        print(f"[verify]   - check group: {name}", flush=True)
        runner()
    print("[verify]   - static policy checks passed", flush=True)


def run_verify_steps(
    build_dir: Path,
    generator: str,
    skip_android: bool,
    format_check: bool,
    format_scope: str,
) -> None:
    run_verify_static_checks()

    if format_check:
        _print_verify_banner(
            f"Step 2/5: running clang-format --check for scope `{format_scope}`"
        )
        cmd_format(
            argparse.Namespace(
                scope=format_scope,
                path=None,
                check=True,
            )
        )
        configure_step = "Step 3/5"
        build_step = "Step 4/5"
        test_step = "Step 5/5"
    else:
        configure_step = "Step 2/4"
        build_step = "Step 3/4"
        test_step = "Step 4/4"

    _print_verify_banner(f"{configure_step}: configuring host build in {build_dir}")
    cmd_configure(
        argparse.Namespace(
            build_dir=str(build_dir),
            generator=generator,
        )
    )

    _print_verify_banner(f"{build_step}: building host targets in {build_dir}")
    cmd_build(
        argparse.Namespace(
            build_dir=str(build_dir),
            configure_if_missing=False,
            generator=generator,
            target=None,
        )
    )

    _print_verify_banner(f"{test_step}: running cargo test for Rust CLI and ctest in {build_dir}")
    cargo_env = os.environ.copy()
    cargo_env["FLIPBITS_CMAKE_BUILD_DIR"] = str(build_dir)
    run(
        [
            "cargo",
            "test",
            "--target",
            RUST_TARGET_TRIPLE,
        ],
        cwd=RUST_CLI_DIR,
        env=cargo_env,
    )
    cmd_test(
        argparse.Namespace(
            build_dir=str(build_dir),
            output_on_failure=True,
            tests_regex=None,
            write_report=True,
            report_dir=None,
        )
    )

    if not skip_android:
        _print_verify_banner("Android step: assembling :app:assembleDebug from the repo root")
        cmd_android(argparse.Namespace(action="assemble-debug", clean=False))


def cmd_verify(args: argparse.Namespace) -> None:
    if getattr(args, "list_checks", False):
        print(format_verify_check_groups())
        return

    build_dir = resolve_build_dir(args.build_dir)
    run_verify_steps(
        build_dir=build_dir,
        generator=args.generator,
        skip_android=args.skip_android,
        format_check=args.format_check,
        format_scope=args.format_scope,
    )
