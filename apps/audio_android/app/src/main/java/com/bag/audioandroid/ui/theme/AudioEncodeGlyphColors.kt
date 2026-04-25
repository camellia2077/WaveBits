package com.bag.audioandroid.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.ui.model.BrandThemeOption

@Immutable
data class AudioEncodeGlyphColors(
    val primarySplit: Color,
    val secondarySplit: Color,
    val outline: Color,
)

private val DefaultAudioEncodeGlyphColors =
    AudioEncodeGlyphColors(
        primarySplit = Color(0xFF9E1B1B),
        secondarySplit = Color(0xFFE8E2D0),
        outline = Color(0xFFC5A059),
    )

val LocalAudioEncodeGlyphColors = staticCompositionLocalOf { DefaultAudioEncodeGlyphColors }

@Composable
@ReadOnlyComposable
fun audioEncodeGlyphColors(): AudioEncodeGlyphColors = LocalAudioEncodeGlyphColors.current

fun audioEncodeGlyphColorsForBrandTheme(theme: BrandThemeOption): AudioEncodeGlyphColors =
    AudioEncodeGlyphColors(
        // The encode glyph is part of the dual-tone surface language: the right split uses the
        // action/accent color, while the left split uses the dominant background color. Keep this
        // generic so newly added dual-tone themes update the gear fill automatically.
        primarySplit = theme.accentColor,
        secondarySplit = theme.backgroundColor,
        outline = theme.outlineColor,
    )

fun defaultAudioEncodeGlyphColors(): AudioEncodeGlyphColors = DefaultAudioEncodeGlyphColors
