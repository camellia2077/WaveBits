package com.bag.audioandroid.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(
    private val appContext: Context
) {
    val selectedPaletteId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[Keys.SelectedPaletteId] }

    val selectedFlashVoicingStyleId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[Keys.SelectedFlashVoicingStyleId] }

    val selectedThemeModeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[Keys.SelectedThemeModeId] }

    val selectedPlaybackSequenceModeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[Keys.SelectedPlaybackSequenceModeId] }

    suspend fun setSelectedPaletteId(paletteId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedPaletteId] = paletteId
        }
    }

    suspend fun setSelectedFlashVoicingStyleId(styleId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedFlashVoicingStyleId] = styleId
        }
    }

    suspend fun setSelectedThemeModeId(themeModeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedThemeModeId] = themeModeId
        }
    }

    suspend fun setSelectedPlaybackSequenceModeId(playbackSequenceModeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedPlaybackSequenceModeId] = playbackSequenceModeId
        }
    }

    private object Keys {
        val SelectedPaletteId: Preferences.Key<String> = stringPreferencesKey("palette_id")
        val SelectedFlashVoicingStyleId: Preferences.Key<String> = stringPreferencesKey("flash_voicing_style")
        val SelectedThemeModeId: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val SelectedPlaybackSequenceModeId: Preferences.Key<String> = stringPreferencesKey("playback_sequence_mode")
    }
}
