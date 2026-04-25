# compare_translation_quality.py

## Purpose

`compare_translation_quality.py` generates `.md` review files with plaintext-style content that compare the English Android string resources with each localized `values-*` resource directory.

It is intended for human or AI review of translation quality. The goal is not strict word-for-word translation. Reviewers should check whether the localized line is natural for the target language and broadly consistent with the English meaning and lineup tone.

## Default paths

- Input: `apps/audio_android/app/src/main/res`
- Output: `temp/ai_translation_reviews`

The script clears the output directory before writing fresh review files, so older flat-layout files do not remain mixed with the newer `app_text` / `sample_text` layout.

The script resolves these paths from the repository root, so it can be run from the repository root with:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py"
```

The unified outer entrypoint is:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py"
```

`compare_translation_quality.py` itself now acts as a library-style module and no longer serves as the direct entrypoint.

## What it generates

For English source text, the script writes `.md` review files under `temp/ai_translation_reviews/en`, first grouped by text type:

- `app_text`: normal app-visible Android strings such as `strings.xml`
- `sample_text`: sample-resource XML files such as `audio_samples_*.xml`

Within each text type, the script keeps the existing sample lineup/category grouping.

For each localized Android resource directory, the script also writes review files grouped the same way. For example:

- `temp/ai_translation_reviews/en/sample_text/ancient_dynasty.md`
- `temp/ai_translation_reviews/ja/sample_text/ancient_dynasty.md`
- `temp/ai_translation_reviews/zh-rTW/sample_text/sacred_machine.md`
- `temp/ai_translation_reviews/de/app_text/other.md`

Each generated file keeps the `.md` extension, but now uses lighter Markdown with only the minimum structure needed for review. The output keeps headings and field labels, while avoiding list-heavy formatting to reduce token cost when pasted into prompts.

English-only source review files look like:

```text
# English Source Review

## Sample Text / Ancient Dynasty
PROMPT: Evaluate whether the English source lines are clear, natural, concise, and tonally consistent...

FILE: audio_samples_ancient_dynasty_somatic_stripping.xml
KEY: audio_sample_ancient_dynasty_themed_alloy_hand_no_warmth
EN: The immortal alloy hand closes, and no warmth answers inside it
```

Localized translation review files look like:

```text
# Translation Review EN vs [JA]

## Sample Text / Ancient Dynasty
PROMPT: Evaluate whether the [JA] lines are natural...

FILE: audio_samples_ancient_dynasty_somatic_stripping.xml
KEY: audio_sample_ancient_dynasty_themed_alloy_hand_no_warmth
EN: The immortal alloy hand closes, and no warmth answers inside it
JA: 不朽合金の手が閉じる。その内側に、もう温度は返らない。
```

The target-language label is parsed from the `values-*` folder name. Examples: `values-ja` becomes `[JA]`, and `values-zh-rTW` becomes `[ZH-RTW]`.

## Text type classification

The script classifies files by filename before writing output:

- `sample_text`: filenames starting with `audio_samples_`
- `app_text`: every other `.xml` resource file included in the comparison set

This allows normal app strings and themed sample strings to be reviewed separately.

## Sample length guard

Sample review output now includes `SAMPLE_LENGTH: SHORT` or `SAMPLE_LENGTH: LONG` for themed sample entries.

This is intentional. In this repository, each sample lineup file contains both short and long sample prose. Reviewers should preserve that length class:

- `SHORT` should stay short and punchy
- `LONG` should stay long and fully developed

Do not approve a translation that turns a short sample into a long paragraph, or collapses a long sample into a short slogan.

## Review standard

The generated prompt asks reviewers to judge whether the target-language text is natural and suitable for that language. In this repository, localized sample text may intentionally adapt sentence structure, rhythm, grammar, and idiom rather than translate every English word exactly.

The generated prompt now also requires JSON-only output so the review result can be consumed by follow-up tooling. The required shape is:

```json
[
  {
    "original": "current text",
    "optimized": "improved text",
    "comment": "short reason"
  }
]
```

If no changes are needed, reviewers should return `[]`.

Acceptable localized text should:

- Keep the broad meaning of the English source.
- Preserve the same lineup tone, such as `Ancient Dynasty`, `Sacred Machine`, or `Labyrinth of Mutability`.
- Sound natural in the target language.
- Prefer language-specific sentence rhythm and grammar over literal English structure.
- Preserve the intended `SHORT` / `LONG` sample length class when the report includes `SAMPLE_LENGTH`.

## Pro sample exception

Pro sample strings use `asciiResId` in `AndroidSampleInputTextProvider.kt`. Their resource keys contain `_ascii_`, for example:

- `audio_sample_ancient_dynasty_ascii_alloy_hand_no_warmth`
- `audio_sample_sacred_machine_ascii_caliper_oil_rite`

These Pro sample texts are fixed ASCII input for the Pro transport mode. They are intentionally English/ASCII in every language, so this script filters them out and does not generate translation comparison entries for them.

## Notes for agents

- Do not treat missing Pro `_ascii_` entries in generated review files as a bug. They are filtered out from both English-only and localized review files.
- If a new sample lineup is added, update `FACTIONS` in the script so output files are grouped correctly.
- If the Android resource path changes, update `DEFAULT_RES_DIRECTORY`.
- If the generated output location changes, update `DEFAULT_OUTPUT_DIRECTORY`.
- XML scanning, resource parsing, and Markdown writing are intentionally shared with `check_mixed_language.py`; keep parsing and output formatting decoupled from report logic.
