package com.bag.audioandroid.ui.screen

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.MorseSpeedOption

@Composable
internal fun MorseSpeedSelectorSection(
    enabled: Boolean,
    selectedMorseSpeed: MorseSpeedOption,
    onMorseSpeedSelected: (MorseSpeedOption) -> Unit,
) {
    SingleChoiceSegmentedButtonRow {
        MorseSpeedOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selectedMorseSpeed == option,
                onClick = { onMorseSpeedSelected(option) },
                enabled = enabled,
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = MorseSpeedOption.entries.size,
                    ),
                colors = appSegmentedButtonColors(),
                label = { Text(text = stringResource(option.labelResId)) },
            )
        }
    }
}
