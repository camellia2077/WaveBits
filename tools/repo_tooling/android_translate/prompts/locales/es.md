# Neutral Spanish Android Translation Locale

[profile_id]
neutral_spanish_standard

[mode]
standard_neutral_spanish

[locale_note]
Spanish (`values-es`) is a neutral Spanish localization. Do not force Spain-only or Latin-America-only wording unless the existing XML already establishes it.

[identity_rule]
Treat this locale as neutral Spanish for broad Spanish-language UI use.
Prefer wording that is understandable across Spanish-speaking regions.
Avoid regional idioms when a neutral product expression works.
Preserve FlipBits protocol tokens such as mini, flash, pro, ultra, ASCII, UTF-8, Hex, Binary, Morse, Hz, payload, token, byte, and nibble when the existing Spanish locale keeps them as technical terms.

[app_text_rule]
For app UI text, keep labels compact enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Follow existing project terms where present: audio, caracteres, bytes, reglas de entrada, Visual, Tokens, Hex, Morse, payload, token, nibble, portadora baja, and portadora alta.
Do not turn short controls, visualizer mode labels, or transport labels into explanatory sentences.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent neutral Spanish.
Keep pro-mode sample strings strict ASCII, without accents, when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write neutral Spanish localization that matches the current XML terminology.
Preserve placeholders and avoid changing number or gender agreement around them unless required by the whole string.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d caracteres • %2$d bytes`.
