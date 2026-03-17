package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.ui.model.UiText

class BagUiTextMapper {
    fun validationIssue(issue: Int): UiText = when (issue) {
        BagApiCodes.VALIDATION_NULL_CONFIG -> UiText.Resource(R.string.validation_null_config)
        BagApiCodes.VALIDATION_NULL_TEXT -> UiText.Resource(R.string.validation_null_text)
        BagApiCodes.VALIDATION_NULL_DECODER_OUTPUT ->
            UiText.Resource(R.string.validation_null_decoder_output)
        BagApiCodes.VALIDATION_INVALID_SAMPLE_RATE ->
            UiText.Resource(R.string.validation_invalid_sample_rate)
        BagApiCodes.VALIDATION_INVALID_FRAME_SAMPLES ->
            UiText.Resource(R.string.validation_invalid_frame_samples)
        BagApiCodes.VALIDATION_INVALID_MODE -> UiText.Resource(R.string.validation_invalid_mode)
        BagApiCodes.VALIDATION_INVALID_FLASH_SIGNAL_PROFILE ->
            UiText.Resource(R.string.validation_invalid_flash_signal_profile)
        BagApiCodes.VALIDATION_INVALID_FLASH_VOICING_FLAVOR ->
            UiText.Resource(R.string.validation_invalid_flash_voicing_flavor)
        BagApiCodes.VALIDATION_PRO_ASCII_ONLY -> UiText.Resource(R.string.validation_pro_ascii_only)
        BagApiCodes.VALIDATION_PAYLOAD_TOO_LARGE ->
            UiText.Resource(R.string.validation_payload_too_large)
        else -> errorCode(BagApiCodes.ERROR_INTERNAL)
    }

    fun errorCode(code: Int): UiText = when (code) {
        BagApiCodes.ERROR_INVALID_ARGUMENT -> UiText.Resource(R.string.error_invalid_argument)
        BagApiCodes.ERROR_NOT_READY -> UiText.Resource(R.string.error_not_ready)
        BagApiCodes.ERROR_NOT_IMPLEMENTED -> UiText.Resource(R.string.error_not_implemented)
        BagApiCodes.ERROR_CANCELLED -> UiText.Resource(R.string.error_cancelled)
        BagApiCodes.ERROR_INTERNAL -> UiText.Resource(R.string.error_internal)
        else -> UiText.Resource(R.string.error_internal)
    }

    fun encodePhaseStatus(modeWireName: String, phase: AudioEncodePhase): UiText = when (phase) {
        AudioEncodePhase.PreparingInput -> UiText.Resource(
            R.string.status_mode_audio_generating_preparing_input,
            listOf(modeWireName)
        )

        AudioEncodePhase.RenderingPcm -> UiText.Resource(
            R.string.status_mode_audio_generating_rendering_pcm,
            listOf(modeWireName)
        )

        AudioEncodePhase.Postprocessing -> UiText.Resource(
            R.string.status_mode_audio_generating_postprocessing,
            listOf(modeWireName)
        )

        AudioEncodePhase.Finalizing -> UiText.Resource(
            R.string.status_mode_audio_generating_finalizing,
            listOf(modeWireName)
        )
    }
}
