package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import com.bag.audioandroid.ui.playerSegmentedButtonColors

@Composable
internal fun FlashSignalVisualizationModeSwitcher(
    selectedMode: FlashSignalVisualizationMode,
    onModeSelected: (FlashSignalVisualizationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("flash-visualization-mode-switcher"),
    ) {
        FlashSignalVisualizationMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = FlashSignalVisualizationMode.entries.size,
                    ),
                colors =
                    playerSegmentedButtonColors(),
                modifier = Modifier.weight(1f),
                label = {
                    Text(text = stringResource(mode.labelResId))
                },
            )
        }
    }
}
