package com.mport.tv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "mport_tv_settings")

class AppSettings(private val context: Context) {

    private val darkKey = booleanPreferencesKey("dark_mode")
    private val playlistKey = stringPreferencesKey("playlist_url")

    val darkMode: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[darkKey] ?: true
    }

    val playlistUrl: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[playlistKey]
    }

    suspend fun setDarkMode(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[darkKey] = value
        }
    }

    suspend fun setPlaylistUrl(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[playlistKey] = value
        }
    }
}
