package org.n3gbx.whisper.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MainDatastore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val isAutoPlayEnabledKey = booleanPreferencesKey("is_auto_play_enabled")
    private val isAutoDownloadEnabledKey = booleanPreferencesKey("is_auto_download_enabled")
    private val isDownloadWifiOnlyEnabledKey = booleanPreferencesKey("is_download_wifi_only_enabled")
    private val installationIdKey = stringPreferencesKey("installation_id")

    suspend fun setAutoPlayEnabled(value: Boolean) = put(isAutoPlayEnabledKey, value)

    suspend fun setAutoDownloadEnabled(value: Boolean) = put(isAutoDownloadEnabledKey, value)

    suspend fun setDownloadWifiOnlyEnabled(value: Boolean) = put(isDownloadWifiOnlyEnabledKey, value)

    suspend fun setInstallationId(value: String) = put(installationIdKey, value)

    fun isDarkTheme() = get(isDarkThemeKey)

    fun isAutoPlayEnabled() = get(isAutoPlayEnabledKey, false)

    fun isAutoDownloadEnabled() = get(isAutoDownloadEnabledKey, false)

    fun isDownloadWifiOnlyEnabled() = get(isDownloadWifiOnlyEnabledKey, false)

    fun getInstallationId() = get(installationIdKey)

    suspend fun setIsDarkTheme(value: Boolean?) {
        if (value == null) remove(isDarkThemeKey)
        else put(isDarkThemeKey, value)
    }

    suspend fun clear() {
        dataStore.edit {
            it.clear()
        }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit {
            it[key] = value
        }
    }

    private suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit {
            it.remove(key = key)
        }
    }

    private fun <T> get(key: Preferences.Key<T>, default: T): Flow<T> = dataStore.data.map { it[key] ?: default }

    private fun <T> get(key: Preferences.Key<T>): Flow<T?> = dataStore.data.map { it[key] }
}