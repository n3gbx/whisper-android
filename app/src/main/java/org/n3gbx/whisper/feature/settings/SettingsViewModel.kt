package org.n3gbx.whisper.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.n3gbx.whisper.BuildConfig
import org.n3gbx.whisper.core.common.GetEpisodesCacheDir
import org.n3gbx.whisper.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val getEpisodesCacheDir: GetEpisodesCacheDir,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _uiEvents = MutableSharedFlow<SettingsUiEvent>()
    val uiEvents: SharedFlow<SettingsUiEvent> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.getAutoPlaySetting(),
                settingsRepository.getAutoDownloadSetting(),
                settingsRepository.getDownloadWifiOnlySetting(),
                settingsRepository.getInstallationId(),
            ) { autoPlay, autoDownload, downloadWifiOnly, installationId ->
                SettingsUiState(
                    autoPlay = autoPlay,
                    autoDownload = autoDownload,
                    downloadWifiOnly = downloadWifiOnly,
                    version = BuildConfig.VERSION_NAME,
                    installationId = installationId
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun onSettingClick(setting: Setting) {
        viewModelScope.launch {
            when {
                setting is Setting.Toggle && setting.type == Setting.Type.AUTO_PLAY ->
                    settingsRepository.setAutoPlaySetting(!setting.value)
                setting is Setting.Toggle && setting.type == Setting.Type.AUTO_DOWNLOAD ->
                    settingsRepository.setAutoDownloadSetting(!setting.value)
                setting is Setting.Toggle && setting.type == Setting.Type.DOWNLOAD_WIFI_ONLY ->
                    settingsRepository.setDownloadWifiOnlySetting(!setting.value)
                setting is Setting.Button && setting.type == Setting.Type.DOWNLOADS ->
                    _uiEvents.emit(SettingsUiEvent.NavigateToDownloads)
                setting is Setting.Link && setting.type == Setting.Type.BACKUP ->
                    _uiEvents.emit(SettingsUiEvent.NavigateToBrowser("https://developer.android.com/identity/data/autobackup#BackupLocation"))
                setting is Setting.Button && setting.type == Setting.Type.CLEAR_DATA ->
                    _uiState.update { it.copy(showClearApplicationDataDialog = true) }
                else -> {}
            }
        }
    }

    fun onClearApplicationDataDialogDismiss() {
        _uiState.update { it.copy(showClearApplicationDataDialog = false) }
    }

    fun onClearApplicationDataDialogConfirm() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { getEpisodesCacheDir().deleteRecursively() }
            settingsRepository.clearLocalData()
            _uiEvents.emit(SettingsUiEvent.Restart)
        }
    }
}