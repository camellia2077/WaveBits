# check_translation_key_alignment.py

## Purpose

`check_translation_key_alignment.py` enforces English-key authority for Android translation resources.

It checks that every localized text XML under `values-*` satisfies both rules:

- localized keys must come from the English `values/` baseline
- localized files must not introduce text XML files that do not exist in English

This is the structural guardrail for workflows where every translation must originate from English.

New keys should enter through `python tools/run.py android strings-add ...`. That wrapper writes only the English baseline by default and then generates these alignment reports so locale-specific translation work follows the agent prompt flow instead of using English fallback text in `values-*`.

## Rules

For each localized `values-*` directory, the checker looks only at translation text XML files:

- `audio_samples_*.xml`
- `strings.xml`
- `strings_*.xml`

It then compares them against the English `values/` baseline.

Generated per-locale task reports may include locale-specific prompt metadata such as:

- `LOCALE_PROFILE`
- `LOCALE_MODE`
- `LOCALE_NOTE`

This allows agents to repair missing keys using the right writing brief for that locale instead of assuming every language follows the same translation style.

Pro `_ascii_` sample keys are a special case: they now live in the shared English file `values/audio_samples_pro_ascii_shared.xml`, remain ASCII for every locale, and are intentionally excluded from missing-key translation tasks.

Issues:

1. missing file
   English has the text XML, but the localized directory does not have that file
2. missing key
   English has the key, localized file exists, but the localized file does not contain that key
3. extra key
   localized file has the key, English does not
4. extra file
   localized text XML exists, but English has no file with the same name

## Missing-file reporting

When an entire localized text XML file is missing, the report now emits a single file-level issue instead of expanding every missing English key into separate entries.

That file-level issue includes:

- `ISSUE: localized file is missing for English base counterpart`
- `MISSING_KEY_COUNT`
- `EXAMPLE_KEYS`

This keeps the report shorter and makes it clearer that the real problem is “the whole localized file is missing”, not “dozens of unrelated key omissions”.

## Usage

From the repository root:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py key-alignment"
```

Or as part of the aggregated reporting flow:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py all"
```

## Output

Reports are written under:

- `temp/translation_key_alignment_reports`

If no issues are found, the tool writes a single OK report.

## Exit codes

- `0`: no alignment issues found
- `2`: one or more missing or extra localized keys/files found
