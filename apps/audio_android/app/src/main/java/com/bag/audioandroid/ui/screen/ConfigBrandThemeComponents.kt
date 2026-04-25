package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

@Composable
internal fun BrandThemeSection(
    accentTokens: AppThemeAccentTokens,
    title: String,
    options: List<BrandThemeOption>,
    selectedBrandTheme: BrandThemeOption,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onBrandThemeSelected: (BrandThemeOption) -> Unit,
) {
    if (options.isEmpty()) {
        return
    }

    val selectedOption = options.firstOrNull { it.id == selectedBrandTheme.id }
    val visibleOptions = if (expanded) options else listOf(selectedOption ?: options.first())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChanged(!expanded) }
                    .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${visibleOptions.size}/${options.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = accentTokens.disclosureAccentTint,
            )
        }
        visibleOptions.forEach { option ->
            BrandThemeRow(
                accentTokens = accentTokens,
                option = option,
                selected = option.id == selectedBrandTheme.id,
                onClick = { onBrandThemeSelected(option) },
            )
        }
    }
}

@Composable
internal fun BrandThemeRow(
    accentTokens: AppThemeAccentTokens,
    option: BrandThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val usesStrongSelectedState = selected && option.id in StrongSelectedBrandThemeIds
    val selectedContainerColor =
        when (option.id) {
            "mars_relic" -> lerp(option.backgroundColor, option.accentColor, 0.10f)
            "scarlet_guard" -> lerp(option.backgroundColor, option.accentColor, 0.14f)
            "black_crimson_rite" -> lerp(option.backgroundColor, option.accentColor, 0.22f)
            else -> MaterialTheme.colorScheme.surface
        }
    Surface(
        color = if (usesStrongSelectedState) selectedContainerColor else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 6.dp else 1.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        border =
            if (selected) {
                BorderStroke(
                    width = if (usesStrongSelectedState) 2.dp else 1.dp,
                    color = accentTokens.selectionBorderAccentTint,
                )
            } else {
                null
            },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandThemePreview(
                backgroundColor = option.backgroundColor,
                accentColor = option.accentColor,
                outlineColor = option.outlineColor,
                contentDescription = stringResource(option.accessibilityLabelResId),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(option.titleResId),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(option.descriptionResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                if (usesStrongSelectedState) {
                    Surface(
                        color =
                            lerp(
                                accentTokens.selectionLabelAccentTint,
                                MaterialTheme.colorScheme.surface,
                                0.78f,
                            ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            SelectedBadge(
                                text = stringResource(R.string.config_palette_selected),
                                tint = accentTokens.selectionLabelAccentTint,
                            )
                        }
                    }
                } else {
                    SelectedBadge(
                        text = stringResource(R.string.config_palette_selected),
                        tint = accentTokens.selectionLabelAccentTint,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BrandThemePreview(
    backgroundColor: Color,
    accentColor: Color,
    outlineColor: Color,
    contentDescription: String,
) {
    Row(
        modifier =
            Modifier
                .size(width = 56.dp, height = 36.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            modifier =
                Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .background(backgroundColor),
        )
        Box(
            modifier =
                Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(accentColor),
        )
        Box(
            modifier =
                Modifier
                    .weight(0.15f)
                    .fillMaxHeight()
                    .background(outlineColor),
        )
    }
}

private val StrongSelectedBrandThemeIds =
    setOf(
        "mars_relic",
        "scarlet_guard",
        "black_crimson_rite",
    )
