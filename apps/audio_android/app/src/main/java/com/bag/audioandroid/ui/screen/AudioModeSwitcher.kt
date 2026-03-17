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
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun AudioModeSwitcher(
    transportMode: TransportModeOption,
    onTransportModeSelected: (TransportModeOption) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        TransportModeOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = transportMode == option,
                onClick = { onTransportModeSelected(option) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TransportModeOption.entries.size
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
                    Text(stringResource(option.labelResId))
                }
            )
        }
    }
}
