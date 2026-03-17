package com.bag.audioandroid.domain

object BagApiCodes {
    const val ERROR_OK = 0
    const val ERROR_INVALID_ARGUMENT = 1
    const val ERROR_NOT_READY = 2
    const val ERROR_NOT_IMPLEMENTED = 3
    const val ERROR_INTERNAL = 4
    const val ERROR_CANCELLED = 5

    const val VALIDATION_OK = 0
    const val VALIDATION_NULL_CONFIG = 1
    const val VALIDATION_NULL_TEXT = 2
    const val VALIDATION_NULL_DECODER_OUTPUT = 3
    const val VALIDATION_INVALID_SAMPLE_RATE = 4
    const val VALIDATION_INVALID_FRAME_SAMPLES = 5
    const val VALIDATION_INVALID_MODE = 6
    const val VALIDATION_PRO_ASCII_ONLY = 7
    const val VALIDATION_PAYLOAD_TOO_LARGE = 8
    const val VALIDATION_INVALID_FLASH_SIGNAL_PROFILE = 9
    const val VALIDATION_INVALID_FLASH_VOICING_FLAVOR = 10
}
