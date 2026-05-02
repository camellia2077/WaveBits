package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.ui.component.ActionButton
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.theme.appThemeAccentTokens

@Composable
internal fun AudioResultCard(
    decodedPayload: DecodedPayloadViewData,
    transportMode: TransportModeOption,
    isCodecBusy: Boolean,
    isDecodeBusy: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    resultContentExpanded: Boolean,
    onToggleResultContentExpanded: () -> Unit,
    onDecode: () -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentTokens = appThemeAccentTokens()

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.audio_result_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentTokens.disclosureAccentTint,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClearResult,
                    enabled =
                        decodedPayload.hasTextResult ||
                            decodedPayload.rawPayloadAvailable ||
                            decodedPayload.textDecodeStatusCode != BagDecodeContentCodes.STATUS_UNAVAILABLE,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.audio_action_clear_result),
                    )
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription =
                            stringResource(
                                if (expanded) {
                                    R.string.audio_action_collapse_result
                                } else {
                                    R.string.audio_action_expand_result
                                },
                            ),
                        tint = accentTokens.disclosureAccentTint,
                    )
                }
            }
            if (expanded) {
                DecodedPayloadContent(
                    accentTokens = accentTokens,
                    decodedPayload = decodedPayload,
                    emptyTextResId = R.string.audio_result_empty,
                    transportMode = transportMode,
                    bodyExpanded = resultContentExpanded,
                    onToggleBodyExpanded = onToggleResultContentExpanded,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActionButton(
                        text =
                            stringResource(
                                if (isDecodeBusy) {
                                    R.string.audio_action_decode_busy
                                } else {
                                    R.string.audio_action_decode
                                },
                            ),
                        onClick = onDecode,
                        enabled = !isCodecBusy,
                        textColor = accentTokens.disclosureAccentTint,
                        borderColor = accentTokens.selectionBorderAccentTint,
                        borderWidth = 2.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
