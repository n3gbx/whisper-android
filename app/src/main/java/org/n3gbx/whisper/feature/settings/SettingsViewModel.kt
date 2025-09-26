package org.n3gbx.whisper.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.n3gbx.whisper.BuildConfig
import org.n3gbx.whisper.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = combine(
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
            theme = "System default", // TODO
            language = "System default", // TODO
            version = BuildConfig.VERSION_NAME,
            installationId = installationId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onSettingClick(setting: Setting) {
        viewModelScope.launch {
            when {
                setting is Setting.Toggle && setting.type == Setting.Type.AUTO_PLAY ->
                    settingsRepository.setAutoPlaySetting(!setting.value)
                setting is Setting.Toggle && setting.type == Setting.Type.AUTO_DOWNLOAD ->
                    settingsRepository.setAutoDownloadSetting(!setting.value)
                setting is Setting.Toggle && setting.type == Setting.Type.DOWNLOAD_WIFI_ONLY ->
                    settingsRepository.setDownloadWifiOnlySetting(!setting.value)
                else -> {} // TODO
            }
        }
    }
}