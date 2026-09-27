package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "astilla_preferences")

class UserPreferencesRepository(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val memoryEnabledKey = booleanPreferencesKey("memory_enabled")
    private val offlineOnlyKey = booleanPreferencesKey("offline_only")
    private val customApiKeyKey = stringPreferencesKey("custom_api_key")
    private val initialSampleLoadedKey = booleanPreferencesKey("initial_sample_loaded")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[themeModeKey]) {
            "DARK" -> ThemeMode.DARK
            "LIGHT" -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
    }

    val memoryEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[memoryEnabledKey] ?: true
    }

    val offlineOnly: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[offlineOnlyKey] ?: false
    }

    val customApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[customApiKeyKey] ?: ""
    }

    val initialSampleLoaded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[initialSampleLoadedKey] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
        }
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[memoryEnabledKey] = enabled
        }
    }

    suspend fun setOfflineOnly(offline: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[offlineOnlyKey] = offline
        }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[customApiKeyKey] = key.trim()
        }
    }

    suspend fun setInitialSampleLoaded(loaded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[initialSampleLoadedKey] = loaded
        }
    }
}
