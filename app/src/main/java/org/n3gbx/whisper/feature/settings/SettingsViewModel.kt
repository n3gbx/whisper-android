package org.n3gbx.whisper.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.BuildConfig
import org.n3gbx.whisper.R
import org.n3gbx.whisper.core.common.GetString
import org.n3gbx.whisper.data.SettingsRepository
import org.n3gbx.whisper.domain.DeleteLocalDataUseCase
import org.n3gbx.whisper.feature.settings.Setting.Type.AUTO_DOWNLOAD
import org.n3gbx.whisper.feature.settings.Setting.Type.AUTO_PLAY
import org.n3gbx.whisper.feature.settings.Setting.Type.CACHE_OPTIMIZATION
import org.n3gbx.whisper.feature.settings.Setting.Type.CLEAR_DATA
import org.n3gbx.whisper.feature.settings.Setting.Type.DOWNLOAD_WIFI_ONLY
import org.n3gbx.whisper.feature.settings.Setting.Type.DOWNLOADS
import org.n3gbx.whisper.model.StringResource.Companion.fromRes
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val deleteLocalData: DeleteLocalDataUseCase,
    private val getString: GetString,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<SettingsUiEvent>()
    val uiEvents: SharedFlow<SettingsUiEvent> = _uiEvents.asSharedFlow()

    init {
        combine(
            settingsRepository.getAutoPlaySetting(),
            settingsRepository.getAutoDownloadSetting(),
            settingsRepository.getDownloadWifiOnlySetting(),
            settingsRepository.getInstallationId(),
            settingsRepository.getCacheOptimizationSetting(),
        ) { autoPlay, autoDownload, downloadWifiOnly, installationId, optimizeCache ->
            SettingsUiState(
                autoPlay = autoPlay,
                autoDownload = autoDownload,
                optimizeCache = optimizeCache,
                downloadWifiOnly = downloadWifiOnly,
                version = BuildConfig.VERSION_NAME,
                installationId = installationId
            )
        }.onEach { _uiState.value = it }.launchIn(viewModelScope)
    }

    fun onSettingClick(setting: Setting) {
        viewModelScope.launch {
            when (setting) {
                is Setting.Toggle -> when (setting.type) {
                    AUTO_PLAY -> settingsRepository.setAutoPlaySetting(!_uiState.value.autoPlay)
                    AUTO_DOWNLOAD -> settingsRepository.setAutoDownloadSetting(!_uiState.value.autoDownload)
                    CACHE_OPTIMIZATION -> settingsRepository.setCacheOptimizationSetting(!_uiState.value.optimizeCache)
                    DOWNLOAD_WIFI_ONLY -> settingsRepository.setDownloadWifiOnlySetting(!_uiState.value.downloadWifiOnly)
                    else -> {}
                }
                is Setting.Link -> _uiEvents.emit(SettingsUiEvent.NavigateToBrowser(setting.url))
                is Setting.Button -> when (setting.type) {
                    DOWNLOADS -> _uiEvents.emit(SettingsUiEvent.NavigateToDownloads)
                    CLEAR_DATA -> _uiState.update { it.copy(showClearApplicationDataDialog = true) }
                    else -> {}
                }
                is Setting.Value<*> -> {}
            }
        }
    }

    fun onClearApplicationDataDialogDismiss() {
        _uiState.update { it.copy(showClearApplicationDataDialog = false) }
    }

    fun onClearApplicationDataDialogConfirm() {
        viewModelScope.launch {
            if (!deleteLocalData()) {
                _uiEvents.emit(SettingsUiEvent.ShowMessage(getString(fromRes(R.string.error_generic))))
            } else {
                _uiEvents.emit(SettingsUiEvent.Restart)
            }
        }
    }
}
