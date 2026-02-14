package org.n3gbx.whisper.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.n3gbx.whisper.database.MainDatabase
import org.n3gbx.whisper.datastore.MainDatastore
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val datastore: MainDatastore,
    private val database: MainDatabase,
) {

    fun getAutoPlaySetting(): Flow<Boolean> = datastore.isAutoPlayEnabled()

    fun getAutoDownloadSetting(): Flow<Boolean> = datastore.isAutoDownloadEnabled()

    fun getDownloadWifiOnlySetting(): Flow<Boolean> = datastore.isDownloadWifiOnlyEnabled()

    fun getCatalogGridLayoutSetting(): Flow<Boolean> = datastore.isCatalogGridLayoutEnabled()

    fun getInstallationId(): Flow<String?> = datastore.getInstallationId()

    suspend fun setAutoPlaySetting(value: Boolean) = datastore.setAutoPlayEnabled(value)

    suspend fun setAutoDownloadSetting(value: Boolean) = datastore.setAutoDownloadEnabled(value)

    suspend fun setDownloadWifiOnlySetting(value: Boolean) = datastore.setDownloadWifiOnlyEnabled(value)

    suspend fun setInstallationId(value: String) = datastore.setInstallationId(value)

    suspend fun setCatalogGridLayoutSetting(value: Boolean) = datastore.setCatalogGridLayoutEnabled(value)

    suspend fun clearLocalData() {
        withContext(Dispatchers.IO) {
            datastore.clear()
            database.clear()
        }
    }
}