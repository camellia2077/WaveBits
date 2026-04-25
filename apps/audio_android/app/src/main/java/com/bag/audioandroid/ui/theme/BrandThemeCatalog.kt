package com.bag.audioandroid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.SampleFlavor

private val BrandInkLight = Color(0xFF241B18)
private val BrandInkDark = Color(0xFFF1E8E1)

val BrandDualToneThemes: List<BrandThemeOption> =
    listOf(
        // Each group is ordered from the most stable / approachable theme to the strongest one.
        // Neighboring entries are also staggered to avoid stacking near-identical hues together.
        brandTheme(
            id = "mars_relic",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_mars_relic_title,
            descriptionResId = R.string.brand_theme_mars_relic_description,
            accessibilityLabelResId = R.string.brand_theme_mars_relic_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            backgroundColor = Color(0xFFE8E2D0),
            accentColor = Color(0xFF9E1B1B),
            outlineColor = Color(0xFFC5A059),
            isDarkTheme = false,
        ),
        brandTheme(
            id = "scarlet_guard",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_scarlet_guard_title,
            descriptionResId = R.string.brand_theme_scarlet_guard_description,
            accessibilityLabelResId = R.string.brand_theme_scarlet_guard_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            backgroundColor = Color(0xFFE0E0E0),
            accentColor = Color(0xFF8B0000),
            outlineColor = Color(0xFFC5A059),
            isDarkTheme = false,
        ),
        brandTheme(
            id = "black_crimson_rite",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_black_crimson_rite_title,
            descriptionResId = R.string.brand_theme_black_crimson_rite_description,
            accessibilityLabelResId = R.string.brand_theme_black_crimson_rite_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            backgroundColor = Color(0xFF4A2B2F),
            accentColor = Color(0xFFDC143C),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "scarlet_carnage",
            groupTitleResId = R.string.config_dual_tone_group_scarlet_carnage,
            titleResId = R.string.brand_theme_scarlet_carnage_title,
            descriptionResId = R.string.brand_theme_scarlet_carnage_description,
            accessibilityLabelResId = R.string.brand_theme_scarlet_carnage_accessibility,
            sampleFlavor = SampleFlavor.ScarletCarnage,
            backgroundColor = Color(0xFF8B0000),
            accentColor = Color(0xFFB8860B),
            outlineColor = Color(0xFF000000),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "labyrinth_of_mutability",
            groupTitleResId = R.string.config_dual_tone_group_labyrinth_of_mutability,
            titleResId = R.string.brand_theme_labyrinth_of_mutability_title,
            descriptionResId = R.string.brand_theme_labyrinth_of_mutability_description,
            accessibilityLabelResId = R.string.brand_theme_labyrinth_of_mutability_accessibility,
            sampleFlavor = SampleFlavor.LabyrinthOfMutability,
            backgroundColor = Color(0xFF005D7C),
            accentColor = Color(0xFFB066FF),
            outlineColor = Color(0xFFFFD700),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "exquisite_fall",
            groupTitleResId = R.string.config_dual_tone_group_exquisite_fall,
            titleResId = R.string.brand_theme_exquisite_fall_title,
            descriptionResId = R.string.brand_theme_exquisite_fall_description,
            accessibilityLabelResId = R.string.brand_theme_exquisite_fall_accessibility,
            sampleFlavor = SampleFlavor.ExquisiteFall,
            backgroundColor = Color(0xFF5C0273),
            accentColor = Color(0xFFAB0040),
            outlineColor = Color(0xFFFFD700),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "immortal_rot",
            groupTitleResId = R.string.config_dual_tone_group_immortal_rot,
            titleResId = R.string.brand_theme_immortal_rot_title,
            descriptionResId = R.string.brand_theme_immortal_rot_description,
            accessibilityLabelResId = R.string.brand_theme_immortal_rot_accessibility,
            sampleFlavor = SampleFlavor.ImmortalRot,
            backgroundColor = Color(0xFF4F7942),
            accentColor = Color(0xFFE4D00A),
            outlineColor = Color(0xFF2D3B2D),
            isDarkTheme = true,
        ),
        // The dark dual-tone dynasty set is separated by material character instead of
        // tiny near-black shifts: alloy leans warm metallic, revival leans jade-cold,
        // sepulcher leans graphite-cyan, and tomb sigil leans earthen tomb black.
        brandTheme(
            id = "ancient_alloy",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_ancient_alloy_title,
            descriptionResId = R.string.brand_theme_ancient_alloy_description,
            accessibilityLabelResId = R.string.brand_theme_ancient_alloy_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            backgroundColor = Color(0xFF423B33),
            accentColor = Color(0xFF00FFCC),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "dynasty_revival",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_dynasty_revival_title,
            descriptionResId = R.string.brand_theme_dynasty_revival_description,
            accessibilityLabelResId = R.string.brand_theme_dynasty_revival_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            // Separate the two green dynasty themes on purpose: dynasty revival stays in
            // a colder jade/bronze lane, while tomb sigil keeps the harsher tomb glow.
            backgroundColor = Color(0xFF31443E),
            accentColor = Color(0xFF00D68F),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "sepulcher_cyan",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_sepulcher_cyan_title,
            descriptionResId = R.string.brand_theme_sepulcher_cyan_description,
            accessibilityLabelResId = R.string.brand_theme_sepulcher_cyan_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            backgroundColor = Color(0xFF2B404A),
            accentColor = Color(0xFF00E5B8),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "tomb_sigil",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_tomb_sigil_title,
            descriptionResId = R.string.brand_theme_tomb_sigil_description,
            accessibilityLabelResId = R.string.brand_theme_tomb_sigil_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            backgroundColor = Color(0xFF4D3C2B),
            accentColor = Color(0xFFA6FF00),
            isDarkTheme = true,
        ),
    )

val DefaultBrandTheme: BrandThemeOption
    get() = BrandDualToneThemes.first()

private fun brandTheme(
    id: String,
    groupTitleResId: Int,
    titleResId: Int,
    descriptionResId: Int,
    accessibilityLabelResId: Int,
    sampleFlavor: SampleFlavor,
    backgroundColor: Color,
    accentColor: Color,
    outlineColor: Color = accentColor,
    isDarkTheme: Boolean,
): BrandThemeOption {
    // Call sites pass colors by visual responsibility, not by Material slot name:
    // backgroundColor is the dominant page/surface color, and accentColor is the
    // strong action/selection color. Do not swap them based on light/dark mode.
    val background = backgroundColor
    // Keep surface fully opaque. Floating UI such as the mini-player relies on
    // MaterialTheme.colorScheme.surface for readable text; adding alpha here makes
    // docked cards look translucent even when the component requests a solid color.
    val surface = backgroundColor
    val onBackground = if (isDarkTheme) BrandInkDark else BrandInkLight
    val primary = accentColor
    val onPrimary = backgroundColor
    val primaryContainer = blend(background, accentColor, if (isDarkTheme) 0.22f else 0.14f)
    val secondaryContainer = blend(background, primary, if (isDarkTheme) 0.30f else 0.18f)
    val surfaceVariant = blend(background, accentColor, if (isDarkTheme) 0.16f else 0.10f)
    val outline = blend(primary, onBackground, if (isDarkTheme) 0.48f else 0.58f)
    val outlineVariant = blend(surfaceVariant, onBackground, if (isDarkTheme) 0.22f else 0.18f)

    val colorScheme =
        if (isDarkTheme) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = BrandInkDark,
                secondary = accentColor,
                onSecondary = backgroundColor,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = BrandInkDark,
                tertiary = accentColor,
                onTertiary = backgroundColor,
                tertiaryContainer = secondaryContainer,
                onTertiaryContainer = BrandInkDark,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = blend(onBackground, accentColor, 0.22f),
                outline = outline,
                outlineVariant = outlineVariant,
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = BrandInkLight,
                secondary = blend(primary, background, 0.20f),
                onSecondary = backgroundColor,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = BrandInkLight,
                tertiary = blend(primary, background, 0.34f),
                onTertiary = backgroundColor,
                tertiaryContainer = blend(backgroundColor, accentColor, 0.08f),
                onTertiaryContainer = BrandInkLight,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = blend(onBackground, accentColor, 0.36f),
                outline = outline,
                outlineVariant = outlineVariant,
            )
        }

    return BrandThemeOption(
        id = id,
        groupTitleResId = groupTitleResId,
        titleResId = titleResId,
        descriptionResId = descriptionResId,
        accessibilityLabelResId = accessibilityLabelResId,
        sampleFlavor = sampleFlavor,
        isDarkTheme = isDarkTheme,
        backgroundColor = backgroundColor,
        accentColor = accentColor,
        outlineColor = outlineColor,
        colorScheme = colorScheme,
    )
}

private fun blend(
    from: Color,
    to: Color,
    ratio: Float,
): Color = lerp(from, to, ratio.coerceIn(0f, 1f))
