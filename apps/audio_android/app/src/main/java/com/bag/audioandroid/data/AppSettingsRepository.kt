package com.bag.audioandroid.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(
    private val appContext: Context,
) {
    val selectedPaletteId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedPaletteId] }

    val selectedFlashVoicingStyleId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedFlashVoicingStyleId] }

    val isFlashVoicingEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.FlashVoicingEnabled] ?: true }

    val selectedThemeModeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedThemeModeId] }

    val selectedThemeStyleId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedThemeStyleId] }

    val selectedBrandThemeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedBrandThemeId] }

    val customBrandThemePresets: Flow<List<CustomBrandThemeSettings>> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                CustomBrandThemeSettingsStore.decode(preferences[Keys.CustomBrandThemePresetsJson])
            }

    val selectedPlaybackSequenceModeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedPlaybackSequenceModeId] }

    val isConfigLanguageExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigLanguageExpanded] ?: true }

    val isConfigThemeAppearanceExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigThemeAppearanceExpanded] ?: true }

    val isDemoModeEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.DemoModeEnabled] ?: false }

    val isSampleDecorationEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SampleDecorationEnabled] ?: true }

    val isFlashVisualPerfOverlayEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.FlashVisualPerfOverlayEnabled] ?: false }

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

    suspend fun setFlashVoicingEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.FlashVoicingEnabled] = enabled
        }
    }

    suspend fun setSelectedThemeModeId(themeModeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedThemeModeId] = themeModeId
        }
    }

    suspend fun setSelectedThemeStyleId(themeStyleId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedThemeStyleId] = themeStyleId
        }
    }

    suspend fun setSelectedBrandThemeId(brandThemeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedBrandThemeId] = brandThemeId
        }
    }

    suspend fun setCustomBrandThemePresets(settings: List<CustomBrandThemeSettings>) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.CustomBrandThemePresetsJson] = CustomBrandThemeSettingsStore.encode(settings)
        }
    }

    suspend fun setSelectedPlaybackSequenceModeId(playbackSequenceModeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedPlaybackSequenceModeId] = playbackSequenceModeId
        }
    }

    suspend fun setConfigLanguageExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigLanguageExpanded] = expanded
        }
    }

    suspend fun setConfigThemeAppearanceExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigThemeAppearanceExpanded] = expanded
        }
    }

    suspend fun setDemoModeEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.DemoModeEnabled] = enabled
        }
    }

    suspend fun setSampleDecorationEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SampleDecorationEnabled] = enabled
        }
    }

    suspend fun setFlashVisualPerfOverlayEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.FlashVisualPerfOverlayEnabled] = enabled
        }
    }

    private object Keys {
        val SelectedPaletteId: Preferences.Key<String> = stringPreferencesKey("palette_id")
        val SelectedFlashVoicingStyleId: Preferences.Key<String> = stringPreferencesKey("flash_voicing_style")
        val FlashVoicingEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("flash_voicing_enabled")
        val SelectedThemeModeId: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val SelectedThemeStyleId: Preferences.Key<String> = stringPreferencesKey("theme_style")
        val SelectedBrandThemeId: Preferences.Key<String> = stringPreferencesKey("brand_theme_id")
        val CustomBrandThemePresetsJson: Preferences.Key<String> = stringPreferencesKey("custom_brand_presets_json")
        val SelectedPlaybackSequenceModeId: Preferences.Key<String> = stringPreferencesKey("playback_sequence_mode")
        val ConfigLanguageExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_language_expanded")
        val ConfigThemeAppearanceExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_theme_appearance_expanded")
        val DemoModeEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("demo_mode_enabled")
        val SampleDecorationEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("sample_decoration_enabled")
        val FlashVisualPerfOverlayEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("visual_fps_overlay_enabled")
    }
}
