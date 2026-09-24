package com.mport.tv.data.settings
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit
private val Context.settingsDataStore by preferencesDataStore("mport_tv_settings")
class AppSettings(private val context:Context){
 private val dark=booleanPreferencesKey("dark_mode"); private val playlist=stringPreferencesKey("playlist_url")
 val darkMode:Flow<Boolean> = context.settingsDataStore.data.map{it[dark]?:true}; val playlistUrl:Flow<String?>=context.settingsDataStore.data.map{it[playlist]}
 suspend fun setDarkMode(v:Boolean)=context.settingsDataStore.edit{it[dark]=v}; suspend fun setPlaylistUrl(v:String)=context.settingsDataStore.edit{it[playlist]=v}
}
