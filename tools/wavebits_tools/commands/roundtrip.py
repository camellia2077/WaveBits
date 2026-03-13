from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path

from .build import cmd_build
from ..artifacts import (
    find_cli_binary,
    slugify,
    test_artifacts_root,
    unique_directory,
    write_json,
    write_utf8,
)
from ..constants import CLI_TARGET_NAME
from ..errors import ToolError
from ..paths import resolve_build_dir
from ..process import run_capture
from ..constants import ROOT_DIR


@dataclass
class RoundtripPaths:
    case_dir: Path
    input_path: Path
    wav_path: Path
    decoded_path: Path
    encode_stdout_path: Path
    encode_stderr_path: Path
    decode_stdout_path: Path
    decode_stderr_path: Path
    meta_path: Path


def resolve_case_dir(args: argparse.Namespace, build_dir: Path, case_name: str) -> Path:
    if args.out_dir:
        path = Path(args.out_dir)
        if not path.is_absolute():
            path = ROOT_DIR / path
        path.mkdir(parents=True, exist_ok=True)
        return path

    base = test_artifacts_root(build_dir) / "roundtrip" / case_name
    return unique_directory(base)


def resolve_roundtrip_paths(case_dir: Path) -> RoundtripPaths:
    return RoundtripPaths(
        case_dir=case_dir,
        input_path=case_dir / "input.txt",
        wav_path=case_dir / "encoded.wav",
        decoded_path=case_dir / "decoded.txt",
        encode_stdout_path=case_dir / "encode.stdout.txt",
        encode_stderr_path=case_dir / "encode.stderr.txt",
        decode_stdout_path=case_dir / "decode.stdout.txt",
        decode_stderr_path=case_dir / "decode.stderr.txt",
        meta_path=case_dir / "meta.json",
    )


def load_input_text(args: argparse.Namespace) -> tuple[str, str]:
    if bool(args.text) == bool(args.text_file):
        raise ToolError("Provide exactly one of --text or --text-file.")

    if args.text_file:
        input_path = Path(args.text_file)
        text = input_path.read_text(encoding="utf-8")
        return text, slugify(input_path.stem, fallback="file")

    return args.text, slugify(args.text, fallback="text")


def ensure_cli_binary(args: argparse.Namespace, build_dir: Path) -> Path:
    cmd_build(
        argparse.Namespace(
            build_dir=str(build_dir),
            configure_if_missing=True,
            generator=args.generator,
            target=[CLI_TARGET_NAME],
            experimental_modules=getattr(args, "experimental_modules", False),
        )
    )
    cli_binary = find_cli_binary(build_dir)
    if cli_binary is None:
        raise ToolError(f"Could not locate `{CLI_TARGET_NAME}` under {build_dir}.")
    return cli_binary


def cmd_roundtrip(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    cli_binary = ensure_cli_binary(args, build_dir)

    input_text, default_name = load_input_text(args)
    case_name = args.case_name or f"{args.mode}-{default_name}"
    case_dir = resolve_case_dir(args, build_dir, slugify(case_name, fallback="roundtrip"))
    paths = resolve_roundtrip_paths(case_dir)

    write_utf8(paths.input_path, input_text)

    encode = run_capture(
        [
            str(cli_binary),
            "encode",
            "--mode",
            args.mode,
            "--text-file",
            str(paths.input_path),
            "--out",
            str(paths.wav_path),
        ]
    )
    write_utf8(paths.encode_stdout_path, encode.stdout)
    write_utf8(paths.encode_stderr_path, encode.stderr)
    if encode.returncode != 0:
        raise ToolError(f"CLI encode failed. See {paths.encode_stderr_path}.")

    decode = run_capture(
        [
            str(cli_binary),
            "decode",
            "--mode",
            args.mode,
            "--in",
            str(paths.wav_path),
            "--out-text",
            str(paths.decoded_path),
        ]
    )
    write_utf8(paths.decode_stdout_path, decode.stdout)
    write_utf8(paths.decode_stderr_path, decode.stderr)
    if decode.returncode != 0:
        raise ToolError(f"CLI decode failed. See {paths.decode_stderr_path}.")

    decoded_text = paths.decoded_path.read_text(encoding="utf-8")
    roundtrip_ok = decoded_text == input_text
    write_json(
        paths.meta_path,
        {
            "mode": args.mode,
            "case_name": case_name,
            "input_path": str(paths.input_path),
            "wav_path": str(paths.wav_path),
            "decoded_path": str(paths.decoded_path),
            "encode_stdout_path": str(paths.encode_stdout_path),
            "encode_stderr_path": str(paths.encode_stderr_path),
            "decode_stdout_path": str(paths.decode_stdout_path),
            "decode_stderr_path": str(paths.decode_stderr_path),
            "cli_binary": str(cli_binary),
            "roundtrip_ok": roundtrip_ok,
            "input_text": input_text,
            "decoded_text": decoded_text,
        },
    )

    print(f"Artifacts: {case_dir}")
    print(f"WAV: {paths.wav_path}")
    print(f"Decoded text: {paths.decoded_path}")
    print(f"Roundtrip: {'OK' if roundtrip_ok else 'MISMATCH'}")
    if not roundtrip_ok:
        raise SystemExit(1)
