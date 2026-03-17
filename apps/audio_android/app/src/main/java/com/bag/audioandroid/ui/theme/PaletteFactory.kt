package com.bag.audioandroid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal fun vividPalette(
    id: String,
    family: PaletteFamily,
    titleResId: Int,
    primary: Color,
    primaryContainer: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    dark: DarkPaletteSeed,
    onPrimaryContainer: Color = Color(0xFF201A1B)
): PaletteOption {
    val lightSecondaryContainer = lerp(primaryContainer, secondary, 0.14f)
    val lightTertiaryContainer = lerp(primaryContainer, tertiary, 0.14f)
    val darkPrimaryContainer = lerp(dark.primary, dark.background, 0.68f)
    val darkSecondaryContainer = lerp(dark.secondary, dark.background, 0.72f)
    val darkTertiaryContainer = lerp(dark.tertiary, dark.background, 0.72f)
    val darkSurfaceVariant = lerp(dark.primary, dark.surface, 0.82f)
    val darkOutline = lerp(dark.surface, Color.White, 0.44f)

    return PaletteOption(
        id = id,
        family = family,
        titleResId = titleResId,
        previewColor = primary,
        lightScheme = lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = lightSecondaryContainer,
            onSecondaryContainer = onPrimaryContainer,
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = lightTertiaryContainer,
            onTertiaryContainer = onPrimaryContainer,
            background = background,
            surface = background
        ),
        darkScheme = darkColorScheme(
            primary = dark.primary,
            onPrimary = Color(0xFF101418),
            primaryContainer = darkPrimaryContainer,
            onPrimaryContainer = lerp(primaryContainer, Color.White, 0.06f),
            secondary = dark.secondary,
            onSecondary = Color(0xFF101418),
            secondaryContainer = darkSecondaryContainer,
            onSecondaryContainer = Color(0xFFF2EAF0),
            tertiary = dark.tertiary,
            onTertiary = Color(0xFF101418),
            tertiaryContainer = darkTertiaryContainer,
            onTertiaryContainer = Color(0xFFF5EBEF),
            background = dark.background,
            onBackground = Color(0xFFEAE2E7),
            surface = dark.surface,
            onSurface = Color(0xFFEAE2E7),
            surfaceVariant = darkSurfaceVariant,
            onSurfaceVariant = Color(0xFFD3C5CC),
            outline = darkOutline
        )
    )
}

internal data class DarkPaletteSeed(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

internal fun darkSeed(
    background: Color,
    surface: Color,
    primary: Color,
    secondary: Color,
    tertiary: Color
): DarkPaletteSeed =
    DarkPaletteSeed(
        background = background,
        surface = surface,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary
    )
