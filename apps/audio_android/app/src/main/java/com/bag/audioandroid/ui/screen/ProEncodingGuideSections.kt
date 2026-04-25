package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.bag.audioandroid.R

@Composable
internal fun ProSymbolNibbleStrip(
    symbols: List<ProSymbolExplanation>,
    currentSymbol: ProSymbolExplanation,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag("pro-symbol-nibble-strip"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.audio_pro_visual_symbol_order),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 1).forEach { slotIndex ->
                val slotSymbol = symbols.firstOrNull { it.slotIndexWithinByte == slotIndex }
                val isActive = currentSymbol.slotIndexWithinByte == slotIndex
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color =
                        if (isActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text =
                                stringResource(
                                    if (slotIndex == 0) {
                                        R.string.audio_pro_visual_step_1
                                    } else {
                                        R.string.audio_pro_visual_step_2
                                    },
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = proSymbolSlotLabel(slotIndex),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.audio_pro_visual_nibble_value, slotSymbol?.nibbleHex ?: "-"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text =
                                stringResource(
                                    if (isActive) {
                                        R.string.audio_pro_visual_playing_now
                                    } else {
                                        R.string.audio_pro_visual_up_next
                                    },
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProPlaybackGuideCard(
    currentSymbol: ProSymbolExplanation,
    nextSymbol: ProUpcomingSymbolExplanation?,
    tokenByteMapping: ProTokenByteMapping,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag("pro-playback-guide-card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_pro_visual_guide_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProGuidePill(
                    title = stringResource(R.string.audio_pro_visual_now),
                    value = stringResource(R.string.audio_pro_visual_symbol_summary, proSymbolSlotLabel(currentSymbol.slotIndexWithinByte), currentSymbol.nibbleHex),
                    modifier = Modifier.fillMaxWidth(),
                )
                ProGuidePill(
                    title = stringResource(R.string.audio_pro_visual_next),
                    value =
                        nextSymbol?.let {
                            stringResource(
                                R.string.audio_pro_visual_symbol_summary,
                                proSymbolSlotLabel(it.slotIndexWithinByte),
                                it.nibbleHex,
                            )
                        } ?: stringResource(R.string.audio_pro_visual_byte_complete),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(
                    R.string.audio_pro_visual_token_mapping,
                    tokenByteMapping.tokenIndex,
                    tokenByteMapping.tokenText.ifBlank { "?" },
                    tokenByteMapping.byteIndexWithinToken + 1,
                    tokenByteMapping.byteCountWithinUnit.coerceAtLeast(1),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text =
                    nextSymbol?.let {
                        stringResource(
                            R.string.audio_pro_visual_next_frequencies,
                            it.lowFreqHz,
                            it.highFreqHz,
                        )
                    } ?: stringResource(R.string.audio_pro_visual_next_byte_advance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProGuidePill(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun proSymbolSlotLabel(slotIndex: Int): String =
    stringResource(
        if (slotIndex == 0) {
            R.string.audio_pro_visual_high_nibble
        } else {
            R.string.audio_pro_visual_low_nibble
        },
    )
