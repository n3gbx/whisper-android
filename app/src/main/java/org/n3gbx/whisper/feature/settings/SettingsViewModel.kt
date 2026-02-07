package org.n3gbx.whisper.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.BuildConfig
import org.n3gbx.whisper.data.SettingsRepository
import org.n3gbx.whisper.model.ApplicationTheme
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.getAutoPlaySetting(),
                settingsRepository.getAutoDownloadSetting(),
                settingsRepository.getDownloadWifiOnlySetting(),
                settingsRepository.getThemeSetting(),
                settingsRepository.getInstallationId()
            ) { autoPlay, autoDownload, downloadWifiOnly, theme, installationId ->
                SettingsUiState(
                    autoPlay = autoPlay,
                    autoDownload = autoDownload,
                    downloadWifiOnly = downloadWifiOnly,
                    theme = theme,
                    language = "System default", // TODO
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
                setting is Setting.Value<*> && setting.type == Setting.Type.THEME ->
                    _uiState.update { it.copy(showThemeOptionsDialog = true) }
                else -> {} // TODO
            }
        }
    }

    fun onApplicationThemeOptionsDialogDismiss() {
        _uiState.update { it.copy(showThemeOptionsDialog = false) }
    }

    fun onApplicationThemeChange(applicationTheme: ApplicationTheme) {
        viewModelScope.launch {
            settingsRepository.setThemeSetting(applicationTheme)
        }
    }
}