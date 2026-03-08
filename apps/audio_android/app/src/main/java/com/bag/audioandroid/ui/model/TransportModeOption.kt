package com.bag.audioandroid.ui.model

enum class TransportModeOption(
    val nativeValue: Int,
    val wireName: String,
    val label: String
) {
    Flash(nativeValue = 0, wireName = "flash", label = "flash"),
    Pro(nativeValue = 1, wireName = "pro", label = "pro"),
    Ultra(nativeValue = 2, wireName = "ultra", label = "ultra")
}
