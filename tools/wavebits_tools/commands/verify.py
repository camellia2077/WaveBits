from __future__ import annotations

import argparse
from pathlib import Path
from typing import Callable

from .android import cmd_android
from .boundary_policy import run_boundary_policy_checks
from .build import cmd_build
from .compatibility_policy import run_compatibility_policy_checks
from .configure import cmd_configure
from .host_import_std_policy import run_host_import_std_policy_checks
from .module_structure_policy import run_module_structure_policy_checks
from .retirement_policy import run_retirement_policy_checks
from .test import cmd_test
from ..paths import resolve_build_dir


VERIFY_CHECK_GROUPS: tuple[tuple[str, str, tuple[str, ...]], ...] = (
    (
        "module_structure",
        "Audit named-module implementation units and module-first wiring.",
        ("core_implementation_modules",),
    ),
    (
        "boundary",
        "Guard CLI/JNI/test allowed surfaces and keep module-first tests from regressing.",
        ("consumer_surfaces", "test_surfaces"),
    ),
    (
        "host_import_std",
        "Guard audited host-only import-std coverage in implementation and module interface files.",
        ("pilot_files", "expansion_files", "core_interfaces", "foundation_modules"),
    ),
    (
        "compatibility",
        "Guard compatibility headers and direct-consumer standardization from drifting back.",
        ("compatibility_sources", "direct_consumers", "outer_headers"),
    ),
    (
        "retirement",
        "Guard boundary-adjacent host wiring, retired wrappers, and explicit legacy carve-out surfaces.",
        ("boundary_host_impl", "wrapper_eviction", "bridge_retirement"),
    ),
)

VERIFY_STATIC_CHECK_RUNNERS: tuple[tuple[str, Callable[[], None]], ...] = (
    ("module_structure", run_module_structure_policy_checks),
    ("boundary", run_boundary_policy_checks),
    ("host_import_std", run_host_import_std_policy_checks),
    ("compatibility", run_compatibility_policy_checks),
    ("retirement", run_retirement_policy_checks),
)


def _print_verify_banner(message: str) -> None:
    print(f"\n[verify] {message}", flush=True)


def format_verify_check_groups() -> str:
    lines = ["Static check groups run before configure/build/test:"]
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
    experimental_modules: bool,
    no_modules: bool,
) -> None:
    run_verify_static_checks()

    _print_verify_banner(f"Step 2/4: configuring host build in {build_dir}")
    cmd_configure(
        argparse.Namespace(
            build_dir=str(build_dir),
            generator=generator,
            experimental_modules=experimental_modules,
            no_modules=no_modules,
        )
    )

    _print_verify_banner(f"Step 3/4: building host targets in {build_dir}")
    cmd_build(
        argparse.Namespace(
            build_dir=str(build_dir),
            configure_if_missing=False,
            generator=generator,
            target=None,
            experimental_modules=experimental_modules,
            no_modules=no_modules,
        )
    )

    _print_verify_banner(f"Step 4/4: running ctest in {build_dir}")
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
        experimental_modules=getattr(args, "experimental_modules", False),
        no_modules=getattr(args, "no_modules", False),
    )
