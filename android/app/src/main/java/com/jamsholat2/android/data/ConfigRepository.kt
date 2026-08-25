package com.jamsholat2.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "jamsholat_config")

class ConfigRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val configKey = stringPreferencesKey("configuration")

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        val stored = prefs[configKey]
        if (stored.isNullOrBlank()) {
            AppConfig.default()
        } else {
            try {
                val decoded = json.decodeFromString<AppConfig>(stored)
                // Migration: if backgroundItems empty but videolist has data, convert
                when {
                    decoded.backgroundItems.isNotEmpty() -> decoded
                    decoded.videolist.isNotEmpty() -> decoded.copy(
                        backgroundItems = decoded.videolist.map { BackgroundItem.fromFileName(it) }
                    )
                    else -> decoded.copy(
                        backgroundItems = listOf(BackgroundItem("tawaf.mp4", "video"))
                    )
                }
            } catch (e: Exception) {
                AppConfig.default()
            }
        }
    }

    suspend fun loadConfig(): AppConfig {
        return configFlow.first()
    }

    suspend fun saveConfig(config: AppConfig): Boolean {
        return try {
            val encoded = json.encodeToString(config)
            context.dataStore.edit { prefs ->
                prefs[configKey] = encoded
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateConfig(transform: (AppConfig) -> AppConfig): AppConfig {
        var newConfig = AppConfig.default()
        context.dataStore.edit { prefs ->
            val raw = prefs[configKey]?.let {
                try { json.decodeFromString<AppConfig>(it) } catch (_: Exception) { AppConfig.default() }
            } ?: AppConfig.default()
            val current = when {
                raw.backgroundItems.isNotEmpty() -> raw
                raw.videolist.isNotEmpty() -> raw.copy(backgroundItems = raw.videolist.map { BackgroundItem.fromFileName(it) })
                else -> raw.copy(backgroundItems = listOf(BackgroundItem("tawaf.mp4", "video")))
            }
            newConfig = transform(current)
            prefs[configKey] = json.encodeToString(newConfig)
        }
        return newConfig
    }

    companion object {
        // Synchronous-like helper for legacy localStorage read parity
        fun buildDefault(): AppConfig = AppConfig.default()
    }
}
