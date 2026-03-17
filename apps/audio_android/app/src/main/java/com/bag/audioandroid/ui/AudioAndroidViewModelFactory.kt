package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.data.AppSettingsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.SavedAudioRepository

class AudioAndroidViewModelFactory(
    private val audioCodecGateway: AudioCodecGateway,
    private val sampleInputTextProvider: SampleInputTextProvider,
    private val appSettingsRepository: AppSettingsRepository,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val savedAudioRepository: SavedAudioRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioAndroidViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioAndroidViewModel(
                audioCodecGateway,
                sampleInputTextProvider,
                appSettingsRepository,
                playbackRuntimeGateway,
                savedAudioRepository
            ) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
