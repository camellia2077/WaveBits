# Italian Android Translation Locale

[profile_id]
italian_standard

[mode]
standard_italian

[locale_note]
Italian (`values-it`) is a standard Italian localization for Android UI, playback, and signal-visualization text.

[identity_rule]
Treat this locale as contemporary Italian UI localization.
Prefer natural Italian sentence order over English structure, especially in playback, input-rule, byte-count, and visualizer strings.
Preserve FlipBits protocol tokens such as mini, flash, pro, ultra, ASCII, UTF-8, Hex, Binary, Morse, Hz, payload, token, byte, and nibble when the existing Italian locale keeps them as technical terms.
For Android string safety, do not introduce raw backslashes, pseudo unicode escapes, or ad-hoc quote escapes. Keep apostrophes/quotes compatible with Android resource parsing and rely on the toolchain escape normalizer before build.

[app_text_rule]
For app UI text, keep labels compact enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Follow existing project terms where present: audio, caratteri, byte, regole di input, Visuale, Tokens, Hex, Morse, payload, token, nibble, portante bassa, and portante alta.
Do not turn short controls, visualizer mode labels, or transport labels into explanatory sentences.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent Italian.
Keep pro-mode sample strings strict ASCII when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Italian localization that matches the current XML terminology.
Preserve placeholders and avoid English-like noun stacking.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d caratteri • %2$d byte`.
Before finalizing `values-it`, run the escape normalization flow so resource strings remain AAPT2-safe.
