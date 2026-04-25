package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

@Composable
internal fun ConfigThemeAppearanceSection(
    selectedThemeStyle: ThemeStyleOption,
    onThemeStyleSelected: (ThemeStyleOption) -> Unit,
    selectedBrandTheme: BrandThemeOption,
    onBrandThemeSelected: (BrandThemeOption) -> Unit,
    selectedThemeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    isExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
    materialPalettes: List<PaletteOption>,
    brandThemes: List<BrandThemeOption>,
    accentTokens: AppThemeAccentTokens,
) {
    val materialPaletteGroups =
        remember(materialPalettes) {
            PaletteFamily.entries.mapNotNull { family ->
                if (family == PaletteFamily.Brand) {
                    return@mapNotNull null
                }
                val options = materialPalettes.filter { it.family == family }
                if (options.isEmpty()) {
                    null
                } else {
                    PaletteGroupUi(
                        family = family,
                        options = options,
                    )
                }
            }
        }
    val sacredMachineThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_sacred_machine
            }
        }
    val ancientDynastyThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_ancient_dynasty
            }
        }
    val immortalRotThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_immortal_rot
            }
        }
    val scarletCarnageThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_scarlet_carnage
            }
        }
    val exquisiteFallThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_exquisite_fall
            }
        }
    val labyrinthOfMutabilityThemes =
        remember(brandThemes) {
            brandThemes.filter {
                it.groupTitleResId == R.string.config_dual_tone_group_labyrinth_of_mutability
            }
        }
    val isBrandStyle = selectedThemeStyle == ThemeStyleOption.BrandDualTone
    var sacredMachineExpanded by rememberSaveable { mutableStateOf(false) }
    var ancientDynastyExpanded by rememberSaveable { mutableStateOf(false) }
    var immortalRotExpanded by rememberSaveable { mutableStateOf(false) }
    var scarletCarnageExpanded by rememberSaveable { mutableStateOf(false) }
    var exquisiteFallExpanded by rememberSaveable { mutableStateOf(false) }
    var labyrinthOfMutabilityExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isBrandStyle, selectedBrandTheme.id, brandThemes) {
        if (!isBrandStyle) {
            return@LaunchedEffect
        }
        when (selectedBrandTheme.groupTitleResId) {
            R.string.config_dual_tone_group_sacred_machine -> sacredMachineExpanded = true
            R.string.config_dual_tone_group_ancient_dynasty -> ancientDynastyExpanded = true
            R.string.config_dual_tone_group_immortal_rot -> immortalRotExpanded = true
            R.string.config_dual_tone_group_scarlet_carnage -> scarletCarnageExpanded = true
            R.string.config_dual_tone_group_exquisite_fall -> exquisiteFallExpanded = true
            R.string.config_dual_tone_group_labyrinth_of_mutability -> labyrinthOfMutabilityExpanded = true
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpandableCardHeader(
                accentTokens = accentTokens,
                title = stringResource(R.string.config_theme_appearance_title),
                subtitle = stringResource(R.string.config_theme_style_subtitle),
                expanded = isExpanded,
                onToggleExpanded = { onExpandedChanged(!isExpanded) },
                contentDescription =
                    stringResource(
                        if (isExpanded) {
                            R.string.config_theme_appearance_collapse
                        } else {
                            R.string.config_theme_appearance_expand
                        },
                    ),
            )

            if (isExpanded) {
                ThemeStyleOption.entries.forEach { option ->
                    SelectionRow(
                        accentTokens = accentTokens,
                        label = stringResource(option.labelResId),
                        selected = option == selectedThemeStyle,
                        onClick = { onThemeStyleSelected(option) },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.config_theme_mode_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.config_theme_mode_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (isBrandStyle) {
                        Text(
                            text = stringResource(R.string.config_theme_mode_brand_fixed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (!isBrandStyle) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        ThemeModeOption.entries.forEach { option ->
                            SelectionRow(
                                accentTokens = accentTokens,
                                label = stringResource(option.labelResId),
                                selected = option == selectedThemeMode,
                                onClick = { onThemeModeSelected(option) },
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.config_palette_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            materialPaletteGroups.forEach { group ->
                                PaletteGroupSection(
                                    accentTokens = accentTokens,
                                    group = group,
                                    selectedPalette = selectedPalette,
                                    onPaletteSelected = onPaletteSelected,
                                )
                            }
                        }
                        Text(
                            text =
                                "${stringResource(selectedPalette.family.titleResId)} · " +
                                    stringResource(selectedPalette.titleResId),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.alpha(0.48f),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        ThemeModeOption.entries.forEach { option ->
                            SelectionRow(
                                accentTokens = accentTokens,
                                label = stringResource(option.labelResId),
                                selected = option == selectedThemeMode,
                                onClick = { onThemeModeSelected(option) },
                                enabled = false,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.config_brand_theme_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.config_brand_theme_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_sacred_machine),
                                options = sacredMachineThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = sacredMachineExpanded,
                                onExpandedChanged = { sacredMachineExpanded = it },
                                onBrandThemeSelected = onBrandThemeSelected,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_ancient_dynasty),
                                options = ancientDynastyThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = ancientDynastyExpanded,
                                onExpandedChanged = { ancientDynastyExpanded = it },
                                onBrandThemeSelected = onBrandThemeSelected,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_immortal_rot),
                                options = immortalRotThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = immortalRotExpanded,
                                onExpandedChanged = { immortalRotExpanded = it },
                                onBrandThemeSelected = onBrandThemeSelected,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_scarlet_carnage),
                                options = scarletCarnageThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = scarletCarnageExpanded,
                                onExpandedChanged = { scarletCarnageExpanded = it },
                                onBrandThemeSelected = onBrandThemeSelected,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_exquisite_fall),
                                options = exquisiteFallThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = exquisiteFallExpanded,
                                onExpandedChanged = { exquisiteFallExpanded = it },
                                onBrandThemeSelected = onBrandThemeSelected,
                            )
                            BrandThemeSection(
                                accentTokens = accentTokens,
                                title = stringResource(R.string.config_dual_tone_group_labyrinth_of_mutability),
                                options = labyrinthOfMutabilityThemes,
                                selectedBrandTheme = selectedBrandTheme,
                                expanded = labyrinthOfMutabilityExpanded,
                                onExpandedChanged = { labyrinthOfMutabilityExpanded = it },
                                onBrandThemeSelected = onBrandThemeSelected,
                            )
                        }
                    }
                }
            }
        }
    }
}
