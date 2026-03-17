package com.bag.audioandroid.data

import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.TransportModeOption

data class SampleInput(
    val id: String,
    val text: String
)

interface SampleInputTextProvider {
    fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption
    ): SampleInput

    fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        excludingSampleId: String? = null
    ): SampleInput

    fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        sampleId: String
    ): SampleInput?
}
