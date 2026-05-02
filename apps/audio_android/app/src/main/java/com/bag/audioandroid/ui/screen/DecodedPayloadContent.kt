package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.analyzeMorseText
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

private enum class DecodedPayloadTab {
    Text,
    Morse,
    Raw,
}

private const val DECODED_PAYLOAD_TAB_TEXT_TAG = "decodedPayloadTabText"
private const val DECODED_PAYLOAD_TAB_MORSE_TAG = "decodedPayloadTabMorse"
private const val DECODED_PAYLOAD_TAB_RAW_TAG = "decodedPayloadTabRaw"

@Composable
internal fun DecodedPayloadContent(
    accentTokens: AppThemeAccentTokens,
    decodedPayload: DecodedPayloadViewData,
    emptyTextResId: Int,
    transportMode: TransportModeOption? = null,
    bodyExpanded: Boolean = true,
    onToggleBodyExpanded: () -> Unit = {},
    startInRawView: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val showMorseTab = transportMode == TransportModeOption.Mini
    val availableTabs =
        if (showMorseTab) {
            DecodedPayloadTab.entries
        } else {
            listOf(DecodedPayloadTab.Text, DecodedPayloadTab.Raw)
        }
    var selectedTabName by rememberSaveable {
        mutableStateOf(if (startInRawView) DecodedPayloadTab.Raw.name else DecodedPayloadTab.Text.name)
    }
    val selectedTab =
        DecodedPayloadTab
            .valueOf(selectedTabName)
            .takeIf { it in availableTabs }
            ?: availableTabs.first()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            availableTabs.forEachIndexed { index, tab ->
                val labelResId =
                    when (tab) {
                        DecodedPayloadTab.Text -> R.string.audio_decode_view_text
                        DecodedPayloadTab.Morse -> R.string.audio_follow_view_morse
                        DecodedPayloadTab.Raw -> R.string.audio_decode_view_raw
                    }
                SegmentedButton(
                    selected = selectedTab == tab,
                    onClick = { selectedTabName = tab.name },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = availableTabs.size,
                        ),
                    colors = appSegmentedButtonColors(),
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag(
                                when (tab) {
                                    DecodedPayloadTab.Text -> DECODED_PAYLOAD_TAB_TEXT_TAG
                                    DecodedPayloadTab.Morse -> DECODED_PAYLOAD_TAB_MORSE_TAG
                                    DecodedPayloadTab.Raw -> DECODED_PAYLOAD_TAB_RAW_TAG
                                },
                            ),
                    label = {
                        Text(stringResource(labelResId))
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    stringResource(
                        when (selectedTab) {
                            DecodedPayloadTab.Text -> R.string.audio_decode_view_text
                            DecodedPayloadTab.Morse -> R.string.audio_follow_view_morse
                            DecodedPayloadTab.Raw -> R.string.audio_decode_view_raw
                        },
                    ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onToggleBodyExpanded) {
                Text(
                    text =
                        stringResource(
                            if (bodyExpanded) {
                                R.string.audio_action_collapse_result_content_short
                            } else {
                                R.string.audio_action_expand_result_content_short
                            },
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = accentTokens.disclosureAccentTint,
                )
                Icon(
                    imageVector = if (bodyExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription =
                        stringResource(
                            if (bodyExpanded) {
                                R.string.audio_action_collapse_result_content
                            } else {
                                R.string.audio_action_expand_result_content
                            },
                        ),
                    tint = accentTokens.disclosureAccentTint,
                )
            }
        }

        if (bodyExpanded) {
            SelectionContainer {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (selectedTab) {
                        DecodedPayloadTab.Text -> {
                            val bodyText =
                                when {
                                    decodedPayload.hasTextResult -> decodedPayload.text
                                    decodedPayload.rawPayloadAvailable &&
                                        decodedPayload.textDecodeStatusCode == BagDecodeContentCodes.STATUS_INVALID_TEXT_PAYLOAD ->
                                        stringResource(R.string.audio_decode_text_unavailable)
                                    else -> stringResource(emptyTextResId)
                                }
                            Text(
                                text = bodyText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        DecodedPayloadTab.Morse -> {
                            val morseText =
                                if (decodedPayload.hasTextResult) {
                                    analyzeMorseText(decodedPayload.text).morseNotation
                                } else {
                                    ""
                                }
                            Text(
                                text = morseText.ifBlank { stringResource(emptyTextResId) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        DecodedPayloadTab.Raw -> {
                            if (!decodedPayload.rawPayloadAvailable) {
                                Text(
                                    text = stringResource(R.string.audio_decode_raw_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.audio_decode_raw_binary_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = decodedPayload.rawBitsBinary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = stringResource(R.string.audio_decode_raw_hex_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = decodedPayload.rawBytesHex,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
