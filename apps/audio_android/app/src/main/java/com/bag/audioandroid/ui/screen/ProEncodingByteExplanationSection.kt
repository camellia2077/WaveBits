package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R

@Composable
internal fun ProByteExplanationCard(
    explanation: ProByteExplanation,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag("pro-byte-explanation-card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.audio_pro_visual_ascii_byte, explanation.byteIndex),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = explanation.tokenText.ifBlank { explanation.asciiDisplay },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.audio_pro_visual_token_byte,
                                explanation.tokenIndex,
                                explanation.byteIndexWithinToken + 1,
                                explanation.byteCountWithinUnit.coerceAtLeast(1),
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = explanation.asciiDisplay,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = stringResource(R.string.audio_pro_visual_byte_binary, explanation.byteHex, explanation.byteBinary),
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProByteSlotIndicator(
                    title = stringResource(R.string.audio_pro_visual_byte_position),
                    active = explanation.isHighNibbleCurrent,
                    nibbleHex = explanation.highNibbleHex,
                    modifier = Modifier.fillMaxWidth(),
                )
                ProByteSlotIndicator(
                    title = stringResource(R.string.audio_pro_visual_then),
                    active = !explanation.isHighNibbleCurrent,
                    nibbleHex = explanation.lowNibbleHex,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProNibbleBadge(
                    title = stringResource(R.string.audio_pro_visual_high_nibble),
                    value = explanation.highNibbleHex,
                    active = explanation.isHighNibbleCurrent,
                )
                ProNibbleBadge(
                    title = stringResource(R.string.audio_pro_visual_low_nibble),
                    value = explanation.lowNibbleHex,
                    active = !explanation.isHighNibbleCurrent,
                )
            }
        }
    }
}

@Composable
private fun ProByteSlotIndicator(
    title: String,
    nibbleHex: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color =
            if (active) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(
                            color =
                                if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            shape = RoundedCornerShape(99.dp),
                        ),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.audio_pro_visual_nibble_value, nibbleHex),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ProNibbleBadge(
    title: String,
    value: String,
    active: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color =
            if (active) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            },
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
