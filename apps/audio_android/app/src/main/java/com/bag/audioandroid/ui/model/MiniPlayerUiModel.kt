package com.bag.audioandroid.ui.model

data class MiniPlayerUiModel(
    val title: UiText,
    val subtitle: UiText,
    val leadingIcon: MiniPlayerLeadingIcon,
    val durationMs: Long,
    val transportMode: TransportModeOption,
    val isFlashMode: Boolean,
    val flashVoicingStyle: FlashVoicingStyleOption?,
    val source: MiniPlayerSource,
)

enum class MiniPlayerLeadingIcon {
    Generated,
    Saved,
}

enum class MiniPlayerSource {
    Generated,
    Saved,
}
