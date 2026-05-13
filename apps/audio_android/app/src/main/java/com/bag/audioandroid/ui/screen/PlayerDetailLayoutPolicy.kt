package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.TransportModeOption

internal data class PlayerDetailLayoutPolicyState(
    val extraLyricsRecoveryHeight: Dp,
    val applyLyricsPreviewBonusLine: Boolean,
)

@Composable
internal fun rememberPlayerDetailLayoutPolicyState(
    transportMode: TransportModeOption,
    playbackDisplayMode: PlaybackDisplayMode,
    displaySlice: PlaybackVerticalSlice?,
    bottomDockSlice: PlaybackVerticalSlice?,
    density: Density,
): PlayerDetailLayoutPolicyState {
    var appliedExtraLyricsRecoveryHeight by remember(transportMode) { mutableStateOf(0.dp) }
    var applyLyricsPreviewBonusLine by remember(transportMode) { mutableStateOf(false) }
    val desiredExtraLyricsRecoveryHeight =
        remember(
            transportMode,
            playbackDisplayMode,
            displaySlice,
            bottomDockSlice,
            appliedExtraLyricsRecoveryHeight,
        ) {
            if (!transportMode.shouldRecoverVisualLyrics(playbackDisplayMode)) {
                0.dp
            } else {
                computeRecoveredLyricsHeight(
                    transportMode = transportMode,
                    displaySlice = displaySlice,
                    bottomDockSlice = bottomDockSlice,
                    density = density,
                    currentAppliedExtra = appliedExtraLyricsRecoveryHeight,
                )
            }
        }
    val desiredLyricsPreviewBonusLine =
        remember(
            transportMode,
            playbackDisplayMode,
            displaySlice,
            bottomDockSlice,
            applyLyricsPreviewBonusLine,
        ) {
            if (!transportMode.shouldApplyTokensBonusLine(playbackDisplayMode)) {
                false
            } else {
                shouldApplyLyricsPreviewBonusLine(
                    displaySlice = displaySlice,
                    bottomDockSlice = bottomDockSlice,
                    density = density,
                    currentlyApplied = applyLyricsPreviewBonusLine,
                )
            }
        }

    LaunchedEffect(transportMode, playbackDisplayMode, desiredExtraLyricsRecoveryHeight) {
        if (!transportMode.shouldRecoverVisualLyrics(playbackDisplayMode)) {
            if (appliedExtraLyricsRecoveryHeight != 0.dp) {
                appliedExtraLyricsRecoveryHeight = 0.dp
            }
            return@LaunchedEffect
        }
        // Lock the recovered lyrics height after the first stable Flash+Visual measurement.
        // Continuously re-applying tiny layout deltas causes visible vertical jitter.
        if (appliedExtraLyricsRecoveryHeight == 0.dp && desiredExtraLyricsRecoveryHeight > 0.dp) {
            appliedExtraLyricsRecoveryHeight = desiredExtraLyricsRecoveryHeight
        }
    }
    LaunchedEffect(transportMode, playbackDisplayMode, desiredLyricsPreviewBonusLine) {
        if (!transportMode.shouldApplyTokensBonusLine(playbackDisplayMode)) {
            if (applyLyricsPreviewBonusLine) {
                applyLyricsPreviewBonusLine = false
            }
            return@LaunchedEffect
        }
        applyLyricsPreviewBonusLine = desiredLyricsPreviewBonusLine
    }

    return remember(appliedExtraLyricsRecoveryHeight, applyLyricsPreviewBonusLine) {
        PlayerDetailLayoutPolicyState(
            extraLyricsRecoveryHeight = appliedExtraLyricsRecoveryHeight,
            applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
        )
    }
}

internal fun TransportModeOption.shouldRecoverVisualLyrics(playbackDisplayMode: PlaybackDisplayMode): Boolean =
    (this == TransportModeOption.Flash || this == TransportModeOption.Mini) &&
        playbackDisplayMode == PlaybackDisplayMode.Visual

internal fun TransportModeOption.shouldApplyTokensBonusLine(playbackDisplayMode: PlaybackDisplayMode): Boolean =
    supportsSharedTokenPage() && playbackDisplayMode == PlaybackDisplayMode.Lyrics

internal fun TransportModeOption.supportsSharedTokenPage(): Boolean =
    this == TransportModeOption.Flash ||
        this == TransportModeOption.Pro ||
        this == TransportModeOption.Ultra

internal val PlayerDetailHorizontalPadding = 24.dp
internal val PlayerDetailLyricsRecoveryPreservedGap = 16.dp
internal val PlayerDetailLyricsRecoveryCap = 96.dp
internal val PlayerDetailMiniVisualLyricsRecoveryCap = 120.dp
internal val PlayerDetailLyricsPreviewBonusPreservedGap = 16.dp
internal val PlayerDetailLyricsPreviewBonusLineCost = 60.dp

internal data class PlaybackVerticalSlice(
    val topPx: Int,
    val bottomPx: Int,
) {
    val heightPx: Int
        get() = (bottomPx - topPx).coerceAtLeast(0)

    fun topDp(density: Density): String = pxToDpString(topPx, density)

    fun heightDp(density: Density): String = pxToDpString(heightPx, density)
}

internal fun LayoutCoordinates.toPlaybackVerticalSlice(): PlaybackVerticalSlice {
    val position = positionInRoot()
    return PlaybackVerticalSlice(
        topPx = position.y.toInt(),
        bottomPx = (position.y + size.height).toInt(),
    )
}

internal fun pxToDpString(
    px: Int,
    density: Density,
): String = with(density) { "%.1f".format(px.toDp().value) }

internal fun computeRecoveredLyricsHeight(
    transportMode: TransportModeOption,
    displaySlice: PlaybackVerticalSlice?,
    bottomDockSlice: PlaybackVerticalSlice?,
    density: Density,
    currentAppliedExtra: Dp,
): Dp {
    val display = displaySlice ?: return 0.dp
    val dock = bottomDockSlice ?: return 0.dp
    val rawGapPx = (dock.topPx - display.bottomPx).coerceAtLeast(0)
    return with(density) {
        val effectiveBaseGap = rawGapPx.toDp() + currentAppliedExtra
        val recoveryCap =
            when (transportMode) {
                TransportModeOption.Mini -> PlayerDetailMiniVisualLyricsRecoveryCap
                else -> PlayerDetailLyricsRecoveryCap
            }
        (effectiveBaseGap - PlayerDetailLyricsRecoveryPreservedGap)
            .coerceAtLeast(0.dp)
            .coerceAtMost(recoveryCap)
    }
}

internal fun shouldApplyLyricsPreviewBonusLine(
    displaySlice: PlaybackVerticalSlice?,
    bottomDockSlice: PlaybackVerticalSlice?,
    density: Density,
    currentlyApplied: Boolean,
): Boolean {
    val display = displaySlice ?: return false
    val dock = bottomDockSlice ?: return false
    val rawGapPx = (dock.topPx - display.bottomPx).coerceAtLeast(0)
    return with(density) {
        val effectiveBaseGap =
            rawGapPx.toDp() + if (currentlyApplied) PlayerDetailLyricsPreviewBonusLineCost else 0.dp
        effectiveBaseGap >= PlayerDetailLyricsPreviewBonusPreservedGap + PlayerDetailLyricsPreviewBonusLineCost
    }
}

internal fun playerDetailAutomationTag(transportMode: TransportModeOption): String =
    when (transportMode) {
        TransportModeOption.Flash -> "FlashAutomation"
        TransportModeOption.Mini -> "MiniAutomation"
        else -> "PlaybackAutomation"
    }
