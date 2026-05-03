package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.theme.appThemeAccentTokens
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FlashPresetPickerSheet(
    selectedStyle: FlashVoicingStyleOption,
    onStyleSelected: (FlashVoicingStyleOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val visualTokens = appThemeVisualTokens()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = visualTokens.modalContainerColor,
        contentColor = visualTokens.modalContentColor,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_flash_voicing_style_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(FlashVoicingStyleOption.entries) { option ->
                    FlashPresetOptionRow(
                        option = option,
                        selected = option == selectedStyle,
                        onClick = { onStyleSelected(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashPresetOptionRow(
    option: FlashVoicingStyleOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accentTokens = appThemeAccentTokens()
    val visualTokens = appThemeVisualTokens()
    val contentColor =
        if (selected) {
            accentTokens.selectionLabelAccentTint
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val borderColor =
        if (selected) {
            accentTokens.selectionBorderAccentTint
        } else {
            Color.Transparent
        }

    Surface(
        color =
            if (selected) {
                visualTokens.selectionSelectedContainerColor
            } else {
                visualTokens.selectionUnselectedContainerColor
            },
        border = if (selected) BorderStroke(SelectedOutlineWidth, borderColor) else null,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(option.labelResId),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor,
                )
                Text(
                    text = stringResource(option.descriptionResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                SelectedBadge(text = stringResource(R.string.config_palette_selected))
            }
        }
    }
}
