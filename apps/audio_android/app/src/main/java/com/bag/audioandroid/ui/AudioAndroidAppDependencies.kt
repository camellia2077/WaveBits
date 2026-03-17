package com.bag.audioandroid.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bag.audioandroid.data.AndroidIntentAudioShareGateway
import com.bag.audioandroid.data.AndroidSampleInputTextProvider
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.data.DefaultSavedAudioRepository
import com.bag.audioandroid.data.MediaStoreAudioExportGateway
import com.bag.audioandroid.data.MediaStoreSavedAudioLibraryGateway
import com.bag.audioandroid.data.NativeAudioCodecGateway
import com.bag.audioandroid.data.NativeAudioIoGateway
import com.bag.audioandroid.data.NativePlaybackRuntimeGateway

@Composable
internal fun rememberAudioAndroidViewModelFactory(appContext: Context): AudioAndroidViewModelFactory {
    val audioCodecGateway = remember { NativeAudioCodecGateway() }
    val audioIoGateway = remember { NativeAudioIoGateway() }
    val sampleInputTextProvider = remember(appContext) {
        AndroidSampleInputTextProvider(appContext)
    }
    val playbackRuntimeGateway = remember { NativePlaybackRuntimeGateway() }
    val audioExportGateway = remember(appContext, audioIoGateway) {
        MediaStoreAudioExportGateway(appContext, audioIoGateway)
    }
    val savedAudioLibraryGateway = remember(appContext, audioIoGateway) {
        MediaStoreSavedAudioLibraryGateway(appContext, audioIoGateway)
    }
    val audioShareGateway = remember(appContext) {
        AndroidIntentAudioShareGateway(appContext)
    }
    val savedAudioRepository = remember(audioExportGateway, savedAudioLibraryGateway, audioShareGateway) {
        DefaultSavedAudioRepository(
            audioExportGateway = audioExportGateway,
            savedAudioLibraryGateway = savedAudioLibraryGateway,
            audioShareGateway = audioShareGateway
        )
    }
    val appSettingsRepository = remember(appContext) {
        AppSettingsRepository(appContext)
    }

    return remember(
        audioCodecGateway,
        sampleInputTextProvider,
        appSettingsRepository,
        playbackRuntimeGateway,
        savedAudioRepository
    ) {
        AudioAndroidViewModelFactory(
            audioCodecGateway = audioCodecGateway,
            sampleInputTextProvider = sampleInputTextProvider,
            appSettingsRepository = appSettingsRepository,
            playbackRuntimeGateway = playbackRuntimeGateway,
            savedAudioRepository = savedAudioRepository
        )
    }
}
