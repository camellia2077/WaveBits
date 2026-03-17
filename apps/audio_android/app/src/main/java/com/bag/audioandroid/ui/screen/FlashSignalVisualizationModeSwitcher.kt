package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
internal fun FlashSignalVisualizationModeSwitcher(
    selectedMode: FlashSignalVisualizationMode,
    onModeSelected: (FlashSignalVisualizationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        FlashSignalVisualizationMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = FlashSignalVisualizationMode.entries.size
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                ),
                modifier = Modifier.weight(1f),
                label = {
                    Text(text = stringResource(mode.labelResId))
                }
            )
        }
    }
}
