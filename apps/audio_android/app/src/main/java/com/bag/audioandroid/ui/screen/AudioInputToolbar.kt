package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@Composable
internal fun AudioInputToolbar(
    enabled: Boolean,
    onOpenInputEditor: () -> Unit,
    sampleInputLength: SampleInputLengthOption,
    onSampleInputLengthSelected: (SampleInputLengthOption) -> Unit,
    onRandomizeSampleInput: () -> Unit,
    onClearInput: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            SampleInputLengthOption.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = sampleInputLength == option,
                    onClick = { onSampleInputLengthSelected(option) },
                    enabled = enabled,
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SampleInputLengthOption.entries.size,
                        ),
                    colors = appSegmentedButtonColors(),
                    label = {
                        Text(text = stringResource(option.labelResId))
                    },
                )
            }
        }
        IconButton(
            onClick = onClearInput,
            enabled = enabled,
            colors = utilityActionIconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.audio_action_clear),
            )
        }
        IconButton(
            onClick = onOpenInputEditor,
            enabled = enabled,
            colors = utilityActionIconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = stringResource(R.string.audio_action_open_input_editor),
            )
        }
        IconButton(
            onClick = onRandomizeSampleInput,
            enabled = enabled,
            colors = utilityActionIconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Casino,
                contentDescription = stringResource(R.string.audio_action_randomize_sample_input),
            )
        }
    }
}
