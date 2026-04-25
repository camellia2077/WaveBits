package com.bag.audioandroid.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption

@Immutable
data class AppThemeAccentTokens(
    val disclosureAccentTint: Color,
    val actionAccentTint: Color,
    val selectionLabelAccentTint: Color,
    val selectionBorderAccentTint: Color,
)

val LocalAppThemeAccentTokens =
    staticCompositionLocalOf {
        AppThemeAccentTokens(
            disclosureAccentTint = Color.Unspecified,
            actionAccentTint = Color.Unspecified,
            selectionLabelAccentTint = Color.Unspecified,
            selectionBorderAccentTint = Color.Unspecified,
        )
    }

@Composable
@ReadOnlyComposable
fun appThemeAccentTokens(): AppThemeAccentTokens = LocalAppThemeAccentTokens.current

fun materialAccentTokens(primary: Color): AppThemeAccentTokens =
    AppThemeAccentTokens(
        disclosureAccentTint = primary,
        actionAccentTint = primary,
        selectionLabelAccentTint = primary,
        selectionBorderAccentTint = primary,
    )

fun brandAccentTokens(theme: BrandThemeOption): AppThemeAccentTokens {
    // Dual-tone themes split their strong state chrome by theme family:
    // dark red rites should read as their original crimson accent instead of near-black, while the
    // dynasty themes keep a restrained energy accent instead of collapsing to the base metal.
    val strongStateTint =
        when {
            theme.id == "black_crimson_rite" ->
                theme.accentColor
            theme.groupTitleResId == R.string.config_dual_tone_group_ancient_dynasty ->
                lerp(
                    theme.accentColor,
                    theme.colorScheme.onSurface,
                    0.32f,
                )
            else -> theme.accentColor
        }
    val actionIconTint =
        lerp(
            theme.accentColor,
            theme.colorScheme.onSurface,
            if (theme.isDarkTheme) 0.18f else 0.32f,
        )
    return AppThemeAccentTokens(
        disclosureAccentTint = strongStateTint,
        actionAccentTint = actionIconTint,
        selectionLabelAccentTint = strongStateTint,
        selectionBorderAccentTint = strongStateTint,
    )
}
