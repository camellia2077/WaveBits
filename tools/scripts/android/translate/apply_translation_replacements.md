# apply_translation_replacements.py

## Purpose

`apply_translation_replacements.py` reads a JSON file of replacement rules and updates Android string XML files under the Android `res` root.

It is intended for controlled bulk text replacement when you already know:

- what the current text should be
- what the replacement text should be

The script is strict by design. Before writing any replacement, it searches the Android `res` tree for a unique `original` match.

This avoids silently overwriting the wrong string when the replacement source is ambiguous.

It also avoids continuing on malformed XML. The script now performs a fail-fast structure check before indexing or replacing any candidate file.

## Default behavior

- Input Android res root: `apps/audio_android/app/src/main/res`
- JSON file: defaults to `tools/scripts/android/translate/replacements.json`
- Target XML lookup: unique lookup by `original` inside `audio_samples_*.xml`

So if you run the script without `--json`, it will try to read:

- `tools/scripts/android/translate/replacements.json`

## JSON format

The JSON file must be a list of objects.

Each object must contain:

- `original`
- `optimized`

Optional:

- `comment`

Example:

```json
[
  {
    "original": "翡翠射線逐一喚出分子鍵之名，並將它們自肉體拆除",
    "optimized": "翡翠射線逐一呼喚分子鍵名，將其自肉胎生生剝離",
    "comment": "tone tightening"
  }
]
```

`comment` is ignored by the replacement script. It is only there for human review context.

## Usage

Run from the repository root:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/apply_translation_replacements.py"
```

That uses the default JSON path:

```text
tools/scripts/android/translate/replacements.json
```

Or pass an explicit JSON file:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/apply_translation_replacements.py --json apps/audio_android/app/src/main/res/values-zh-rTW/replacements.json"
```

Optional custom res root:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/apply_translation_replacements.py --res-dir apps/audio_android/app/src/main/res --json apps/audio_android/app/src/main/res/values-zh-rTW/replacements.json"
```

## Validation rules

For each JSON entry, the script checks:

1. exactly one `<string>` text under sample XML files (`audio_samples_*.xml`) exactly equals `original`
2. that matched string is then updated to `optimized`

If no match is found, or if multiple matches are found, that entry is reported as an error and is not applied.

This means the script assumes `original` should point to a single source string in the sample-text resource tree.

Before replacement, candidate XML files are also checked for structure problems:

1. root tag must be `<resources>`
2. there must be no unexpected text directly under `<resources>`
3. there must be no unexpected trailing text after `<string>` elements

If a file fails these checks, the script stops using that file and reports the exact reason.

The same structure check is run twice:

1. before replacement
2. after generating the would-be updated XML text

Only when both checks pass will the file actually be written.

## Output behavior

The script prints:

- total applied replacement count
- character-level colored diffs for each applied replacement
- validation errors, if any

Exit codes:

- `0`: all replacements applied successfully
- `1`: invalid path or setup error
- `2`: one or more validation errors occurred

## Notes

- `original` must match the current XML text exactly, including punctuation differences.
- This script only scans and updates sample XML files named `audio_samples_*.xml`.
- This scope is intentional so unrelated resources such as `themes.xml` or `ic_launcher_colors.xml` do not produce noise or get touched by the replacement flow.
- The script now replaces only the matched `<string>` inner text and does not rewrite the full XML formatting.
- If the same `original` text appears in multiple XML entries, the script will report ambiguity instead of guessing.
- For speed, the script scans `values` and `values-*` once, builds an in-memory index of source texts, and reuses cached file contents during replacement.
- After applying replacements, the script prints a terminal diff with red removed characters and green added characters.
- The fail-fast XML check was added because earlier malformed files could otherwise keep being modified and hide the real source of the corruption.
- The replacement is only committed when both the original XML and the post-replacement XML pass the same structure validation.
