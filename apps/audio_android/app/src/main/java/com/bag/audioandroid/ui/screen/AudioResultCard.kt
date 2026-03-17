package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.bag.audioandroid.ui.component.ActionButton

@Composable
internal fun AudioResultCard(
    resultText: String,
    isCodecBusy: Boolean,
    isDecodeBusy: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDecode: () -> Unit,
    onClearInput: () -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resultScrollState = rememberScrollState()

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.audio_result_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClearResult,
                    enabled = resultText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.audio_action_clear_result)
                    )
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) {
                                R.string.audio_action_collapse_result
                            } else {
                                R.string.audio_action_expand_result
                            }
                        )
                    )
                }
            }
            if (expanded) {
                if (resultText.isBlank()) {
                    Text(
                        text = stringResource(R.string.audio_result_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(resultScrollState)
                    ) {
                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = stringResource(
                        if (isDecodeBusy) {
                            R.string.audio_action_decode_busy
                        } else {
                            R.string.audio_action_decode
                        }
                    ),
                    onClick = onDecode,
                    enabled = !isCodecBusy,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = stringResource(R.string.audio_action_clear),
                    onClick = onClearInput,
                    enabled = !isCodecBusy,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
