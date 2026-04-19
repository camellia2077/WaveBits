from __future__ import annotations

from collections import OrderedDict
from dataclasses import dataclass
import re
from pathlib import Path
import shutil

from .artifacts import write_json, write_utf8


ANSI_ESCAPE_PATTERN = re.compile(r"\x1b\[[0-9;]*[A-Za-z]")
DIAGNOSTIC_LINE_PATTERN = re.compile(
    r"^((?:[A-Za-z]:)?[^:\n]+):(\d+):(\d+):\s+(warning|error|note|remark):\s+(.+)$"
)
CHECK_NAME_EXTRACTOR = re.compile(r"(.+)\s+\[([^\]]+)\]$")
TASKS_PER_PATCH_DIR = 10


@dataclass(frozen=True, slots=True)
class TidyDiagnostic:
    file_path: str
    line: int
    column: int
    severity: str
    message: str
    check: str
    raw_lines: tuple[str, ...]


@dataclass(frozen=True, slots=True)
class TidyTask:
    task_id: str
    source_file: str
    checks: tuple[str, ...]
    diagnostics: tuple[TidyDiagnostic, ...]


def parse_diagnostics(log_content: str) -> list[TidyDiagnostic]:
    diagnostics: list[TidyDiagnostic] = []
    current: dict | None = None
    for raw_line in log_content.splitlines():
        stripped = ANSI_ESCAPE_PATTERN.sub("", raw_line).strip()
        match = DIAGNOSTIC_LINE_PATTERN.match(stripped)
        if match:
            severity = match.group(4)
            if severity == "note" and current is not None:
                current["raw_lines"].append(raw_line)
                continue

            full_message = match.group(5).strip()
            check_match = CHECK_NAME_EXTRACTOR.match(full_message)
            if check_match:
                message = check_match.group(1).strip()
                check_name = check_match.group(2).strip()
            else:
                message = full_message
                check_name = f"clang-diagnostic-{severity}"

            current = {
                "file_path": match.group(1),
                "line": int(match.group(2)),
                "column": int(match.group(3)),
                "severity": severity,
                "message": message,
                "check": check_name,
                "raw_lines": [raw_line],
            }
            diagnostics.append(
                TidyDiagnostic(
                    file_path=current["file_path"],
                    line=current["line"],
                    column=current["column"],
                    severity=current["severity"],
                    message=current["message"],
                    check=current["check"],
                    raw_lines=tuple(current["raw_lines"]),
                )
            )
            current = {
                **current,
                "diagnostic_index": len(diagnostics) - 1,
            }
            continue

        if current is None:
            continue

        current["raw_lines"].append(raw_line)
        index = current["diagnostic_index"]
        previous = diagnostics[index]
        diagnostics[index] = TidyDiagnostic(
            file_path=previous.file_path,
            line=previous.line,
            column=previous.column,
            severity=previous.severity,
            message=previous.message,
            check=previous.check,
            raw_lines=tuple(current["raw_lines"]),
        )

    return diagnostics


def build_tasks(log_content: str) -> list[TidyTask]:
    tasks_by_file: OrderedDict[str, list[TidyDiagnostic]] = OrderedDict()
    for diagnostic in parse_diagnostics(log_content):
        tasks_by_file.setdefault(diagnostic.file_path, []).append(diagnostic)

    tasks: list[TidyTask] = []
    for index, (source_file, diagnostics) in enumerate(tasks_by_file.items(), start=1):
        checks: list[str] = []
        for diagnostic in diagnostics:
            if diagnostic.check and diagnostic.check not in checks:
                checks.append(diagnostic.check)
        tasks.append(
            TidyTask(
                task_id=f"{index:03d}",
                source_file=source_file,
                checks=tuple(checks),
                diagnostics=tuple(diagnostics),
            )
        )
    return tasks


def write_task_artifacts(output_root: Path, tasks: list[TidyTask]) -> dict:
    tasks_root = output_root / "tasks"
    if tasks_root.exists():
        shutil.rmtree(tasks_root)
    tasks_root.mkdir(parents=True, exist_ok=True)

    task_records: list[dict] = []
    for task in tasks:
        patch_group_index = ((int(task.task_id) - 1) // TASKS_PER_PATCH_DIR) + 1
        task_rel_path = (
            Path("tasks")
            / f"patch_{patch_group_index:03d}"
            / f"{task.task_id}.log"
        )
        write_utf8(output_root / task_rel_path, render_task_log(task))
        task_records.append(
            {
                "task_id": task.task_id,
                "source_file": task.source_file,
                "diagnostic_count": len(task.diagnostics),
                "checks": list(task.checks),
                "task_log": str(task_rel_path).replace("\\", "/"),
            }
        )

    summary = {
        "task_count": len(tasks),
        "tasks": task_records,
    }
    write_json(output_root / "summary.json", summary)
    return summary


def render_task_log(task: TidyTask) -> str:
    lines = [
        f"Task: {task.task_id}",
        f"Source: {task.source_file}",
        f"Checks: {', '.join(task.checks) if task.checks else '<none>'}",
        f"Diagnostics: {len(task.diagnostics)}",
        "ClangTidyConfig: ../../.clang-tidy",
        "ClangFormatConfig: ../../.clang-format",
        "",
    ]
    for index, diagnostic in enumerate(task.diagnostics):
        lines.extend(diagnostic.raw_lines)
        if index != len(task.diagnostics) - 1:
            lines.append("")
    lines.extend(
        [
            "",
            "# Patch Goal",
            "- Apply a minimal fix for the diagnostics listed above.",
            "- Format touched code with the bundled .clang-format before finalizing.",
            "",
        ]
    )
    return "\n".join(lines).rstrip() + "\n"
