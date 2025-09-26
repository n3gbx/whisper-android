package org.n3gbx.whisper.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import org.n3gbx.whisper.datastore.MainDatastore
import org.n3gbx.whisper.model.ApplicationTheme
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val datastore: MainDatastore
) {

    fun getAutoPlaySetting(): Flow<Boolean> = datastore.isAutoPlayEnabled()

    fun getAutoDownloadSetting(): Flow<Boolean> = datastore.isAutoDownloadEnabled()

    fun getDownloadWifiOnlySetting(): Flow<Boolean> = datastore.isDownloadWifiOnlyEnabled()

    fun getInstallationId(): Flow<String?> = datastore.getInstallationId()

    fun getThemeSetting(): Flow<ApplicationTheme> = datastore.isDarkTheme()
        .mapLatest {
            when (it) {
                true -> ApplicationTheme.DARK
                false -> ApplicationTheme.LIGHT
                null -> ApplicationTheme.SYSTEM
            }
        }

    suspend fun setAutoPlaySetting(value: Boolean) = datastore.setAutoPlayEnabled(value)

    suspend fun setAutoDownloadSetting(value: Boolean) = datastore.setAutoDownloadEnabled(value)

    suspend fun setDownloadWifiOnlySetting(value: Boolean) = datastore.setDownloadWifiOnlyEnabled(value)

    suspend fun setInstallationId(value: String) = datastore.setInstallationId(value)

    suspend fun setThemeSetting(value: ApplicationTheme) {
        when (value) {
            ApplicationTheme.DARK -> datastore.setIsDarkTheme(true)
            ApplicationTheme.LIGHT -> datastore.setIsDarkTheme(false)
            ApplicationTheme.SYSTEM -> datastore.setIsDarkTheme(null)
        }
    }

    suspend fun resetSettings() = datastore.clear()
}