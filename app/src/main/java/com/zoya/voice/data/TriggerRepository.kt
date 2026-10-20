package com.zoya.voice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoya.voice.domain.model.Trigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.triggerDataStore: DataStore<Preferences> by preferencesDataStore(name = "zoya_triggers")

class TriggerRepository(private val context: Context) {

    private val KEY = stringPreferencesKey("triggers_json")

    val triggers: Flow<List<Trigger>> = context.triggerDataStore.data.map { prefs ->
        val json = prefs[KEY]
        if (json.isNullOrBlank()) defaultTriggers() else parse(json)
    }

    suspend fun save(triggers: List<Trigger>) {
        context.triggerDataStore.edit { prefs ->
            prefs[KEY] = toJson(triggers)
        }
    }

    private fun defaultTriggers(): List<Trigger> = listOf(
        Trigger("1", "Open YouTube", "open youtube", "openWebsite:https://youtube.com", true),
        Trigger("2", "Search Google", "search for", "search", true),
        Trigger("3", "Set Reminder", "remind me", "reminder", true)
    )

    private fun toJson(list: List<Trigger>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("phrase", t.phrase)
                put("action", t.action)
                put("enabled", t.enabled)
            })
        }
        return arr.toString()
    }

    private fun parse(json: String): List<Trigger> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Trigger(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    phrase = obj.getString("phrase"),
                    action = obj.getString("action"),
                    enabled = obj.getBoolean("enabled")
                )
            }
        } catch (e: Exception) {
            defaultTriggers()
        }
    }
}