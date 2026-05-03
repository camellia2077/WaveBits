package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.theme.appThemeAccentTokens

@Composable
internal fun FlashVoicingSelectorSection(
    enabled: Boolean,
    isFlashVoicingEnabled: Boolean,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
) {
    val effectiveSelectedStyle =
        if (isFlashVoicingEnabled) {
            selectedFlashVoicingStyle
        } else {
            FlashVoicingStyleOption.Steady
        }
    var isVoicingStyleSheetOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlashPresetEntryRow(
            title = stringResource(R.string.audio_flash_voicing_style_title),
            value = stringResource(effectiveSelectedStyle.labelResId),
            contentDescription = stringResource(R.string.audio_action_select_flash_voicing_style),
            enabled = enabled,
            onClick = { isVoicingStyleSheetOpen = true },
        )
        Text(
            text = stringResource(R.string.audio_flash_style_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (isVoicingStyleSheetOpen) {
        FlashPresetPickerSheet(
            selectedStyle = effectiveSelectedStyle,
            onStyleSelected = { option ->
                onFlashVoicingStyleSelected(option)
                isVoicingStyleSheetOpen = false
            },
            onDismiss = { isVoicingStyleSheetOpen = false },
        )
    }
}

@Composable
private fun FlashPresetEntryRow(
    title: String,
    value: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accentTokens = appThemeAccentTokens()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { this.contentDescription = contentDescription }
                .padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = accentTokens.selectionLabelAccentTint,
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = accentTokens.disclosureAccentTint,
        )
    }
}
