package com.zoya.voice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zoya_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        val GROK_KEY = stringPreferencesKey("grok_api_key")
        val OPENROUTER_KEY = stringPreferencesKey("openrouter_api_key")
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_NAME] ?: "Babe"
    }

    val geminiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.GEMINI_KEY] ?: ""
    }

    val grokKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.GROK_KEY] ?: ""
    }

    val openRouterKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.OPENROUTER_KEY] ?: ""
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim().ifEmpty { "Babe" } }
    }

    suspend fun saveGeminiKey(key: String) {
        context.dataStore.edit { it[Keys.GEMINI_KEY] = key.trim() }
    }

    suspend fun saveGrokKey(key: String) {
        context.dataStore.edit { it[Keys.GROK_KEY] = key.trim() }
    }

    suspend fun saveOpenRouterKey(key: String) {
        context.dataStore.edit { it[Keys.OPENROUTER_KEY] = key.trim() }
    }

    suspend fun saveAll(
        userName: String,
        gemini: String,
        grok: String,
        openRouter: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = userName.trim().ifEmpty { "Babe" }
            prefs[Keys.GEMINI_KEY] = gemini.trim()
            prefs[Keys.GROK_KEY] = grok.trim()
            prefs[Keys.OPENROUTER_KEY] = openRouter.trim()
        }
    }
}