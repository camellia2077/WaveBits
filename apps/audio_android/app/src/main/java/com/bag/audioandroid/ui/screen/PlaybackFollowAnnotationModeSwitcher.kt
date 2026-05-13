package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerSegmentedButtonColors

@Composable
internal fun PlaybackFollowAnnotationModeSwitcher(
    selectedMode: PlaybackFollowViewMode,
    onModeSelected: (PlaybackFollowViewMode) -> Unit,
    transportMode: TransportModeOption?,
    modifier: Modifier = Modifier,
) {
    val options =
        if (transportMode == TransportModeOption.Mini) {
            listOf(PlaybackFollowViewMode.Morse)
        } else {
            listOf(
                PlaybackFollowViewMode.Binary,
                PlaybackFollowViewMode.Hex,
            )
        }
    SingleChoiceSegmentedButtonRow(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("follow-annotation-switcher"),
    ) {
        options.forEachIndexed { index, option ->
            val optionLabel = stringResource(option.titleResId)
            SegmentedButton(
                selected = selectedMode == option,
                onClick = { onModeSelected(option) },
                modifier =
                    Modifier
                        .testTag("follow-annotation-${option.name.lowercase()}")
                        .semantics { contentDescription = optionLabel },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                colors = playerSegmentedButtonColors(),
                label = { Text(text = optionLabel) },
            )
        }
    }
}
