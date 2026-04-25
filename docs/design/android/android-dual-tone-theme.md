# Android Dual-tone Theme

Related:

- [Android Encode Glyph Colors](./android-encode-glyph-colors.md)

## Goal

`双色主题` 不把两种颜色平均铺到所有控件上，而是固定成：

- `base color`
  - 负责背景气质、surface 层次和整体氛围
- `accent color`
  - 负责局部强调、选中状态、图标和 disclosure chrome

## Rules

- 大面积背景和卡片层次优先跟 `base color`
- 小面积强调优先跟 `accent color`
- 正文文本和分隔线优先从 `ColorScheme` 的可读性色推导
- 不让 `accent color` 直接铺满整块卡片或正文区域
- `Material` 风格继续走单色策略
- `双色主题` 通过额外 UI accent tokens 把第二色接到局部 chrome 和选中态

## Theme Types

### Light base + solid accent

Examples:

- `Mars Relic`
- `Scarlet Guard`

Rules:

- `disclosure arrow`
  - use `accent` directly or a slightly deeper variant
- `selected label`
  - use `accent`
- `selected border`
  - use `accent`
- `action icon`
  - use a lighter version of `accent`
  - typically `accent` mixed with `onSurface`

### Dark base + solid accent

Examples:

- `Black Crimson Rite`

Rules:

- `disclosure arrow`
  - use a darker, heavier `accent`
- `selected label`
  - use a darker, heavier `accent`
- `selected border`
  - use a darker, heavier `accent`
- `action icon`
  - do not use the heaviest accent directly
  - typically `accent` mixed with `onSurface`

### Dark base + energy accent

Examples:

- `Dynasty Revival`
- `Sepulcher Cyan`
- `Ancient Alloy`

Rules:

- `disclosure arrow`
  - use a restrained bright accent
  - typically `accent` mixed with `onSurface`
- `selected label`
  - use a stronger accent than the arrow
- `selected border`
  - use a stronger accent than the arrow
- `action icon`
  - use the most restrained accent variant
  - typically more mixed with `onSurface` than the arrow

## Current Mapping

- `Material`
  - all current UI accent tokens use `primary`
- `Dual-tone`
  - strong stateful chrome uses the stronger side of the theme
  - small utility action icons use a lighter accent derived from the paired color and `onSurface`
  - bottom navigation is handled as a `container + foreground` pair, not with disclosure / selection accent tokens

## Strong Selected Treatments

- `Config` dual-tone theme rows
  - some themes need a stronger selected state to stay distinct from the surrounding card
  - selected rows can use all three together:
    - higher-contrast container
    - stronger outline
    - clearer selected badge
- `Audio` flash voicing rows
  - follow the same stronger selected-state pattern as Config rows
  - selected state should read as a structured choice, not only a text color change
- `Audio` primary action buttons
  - buttons normally stay on the standard container treatment
  - a small number of stronger actions can add a clearer outline when they need more separation from nearby surfaces

## Bottom Navigation

- `Material`
  - selected state follows the normal Material mapping
  - unselected state uses `primary` with reduced alpha
- `Dual-tone`
  - dark-base themes keep unselected icon/text on the brighter paired color directly
  - light-base themes slightly correct the brighter paired color toward `onPrimaryContainer`
    - this preserves the dual-tone relationship while keeping the foreground distinct from the navigation container
  - selected icon/text/indicator are mapped explicitly from the original dual-tone pair
  - this keeps navigation in the same two-color language instead of falling back to derived alpha variants

## Current Tokens

- `disclosureAccentTint`
  - 展开箭头、关于页箭头这类 disclosure chrome
  - 优先承担“状态性强”的展开指示
- `actionAccentTint`
  - 输入卡片里的 `TXT` / 骰子等小型 action icon
  - 比 `disclosureAccentTint` 更克制
- `selectionLabelAccentTint`
  - 已选标签文字
  - 比普通正文更强，但不直接接管正文系统
- `selectionBorderAccentTint`
  - 选中边框、选中描边
  - 用于选中态的结构性强调

## Color Role Names

`BrandThemeCatalog.kt` uses visual-role names rather than Material slot names:

- `backgroundColor`
  - dominant page / surface color
  - used for large-area atmosphere and dual-tone preview's left side
- `accentColor`
  - primary interaction and selection color
  - used for buttons, selected states, disclosure accents, and dual-tone preview's right side
- `outlineColor`
  - small-area graphic / mechanical highlight color
  - currently used by the Audio encode gear glyph outline
  - defaults to `accentColor` when a theme does not need a separate outline accent

Do not infer color responsibility from `isDarkTheme`. `isDarkTheme` only selects readable Material color-scheme behavior; it must not swap the semantic meaning of `backgroundColor` and `accentColor`.

## Audio Encode Glyph

- The Audio encode gear glyph is part of the dual-tone language.
- Fill colors come from `AudioEncodeGlyphColors`:
  - `primarySplit` = `accentColor`
  - `secondarySplit` = `backgroundColor`
  - `outline` = `outlineColor`
- Do not hard-code theme-specific gear colors inside glyph drawing components.
- If a new theme needs better gear contrast, set `outlineColor` in `BrandThemeCatalog.kt` first.

## Adding Or Changing A Dual-tone Theme

When adding a new dual-tone theme or changing theme colors:

1. Read this document first.
2. Update `BrandThemeCatalog.kt` with:
   - `backgroundColor`
   - `accentColor`
   - optional `outlineColor`
   - `sampleFlavor`
3. If adding a new group / lineup, update all visible string resources for the group and theme names.
4. If adding a new text flavor, update:
   - `AndroidSampleInputTextProvider.kt`
   - matching `audio_samples_*` resources for every supported language
   - tests or fake providers that exhaustively switch on `SampleFlavor`
5. If adding a new Config group, wire it into `ConfigThemeAppearanceSection.kt` so it participates in collapsible dual-tone grouping.
6. Keep concrete UI components free of theme-id color branches; route color behavior through shared helpers / tokens.
7. Run the focused Android verification used by nearby theme/sample changes.

