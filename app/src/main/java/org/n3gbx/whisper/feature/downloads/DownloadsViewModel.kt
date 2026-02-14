package org.n3gbx.whisper.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.domain.DeleteEpisodeCacheUseCase
import org.n3gbx.whisper.domain.DeleteEpisodesCacheUseCase
import org.n3gbx.whisper.model.Episode
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val deleteEpisodeCache: DeleteEpisodeCacheUseCase,
    private val deleteEpisodesCache: DeleteEpisodesCacheUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<DownloadsUiEvent>()
    val uiEvents: SharedFlow<DownloadsUiEvent> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            episodeRepository
                .getDownloadedEpisodes()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .map { episodes ->
                    episodes.map { episode ->
                        val fileSizeMb = episode.getFileSizeMb()?.let { "$it Mb" } ?: "??? Mb"
                        episode to fileSizeMb
                    }
                }
                .collect { episodes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            episodes = episodes,
                        )
                    }
                }
        }
    }

    private fun Episode.getFileSizeMb(): Float? =
        localPath?.let { File(it) }?.length()?.let { it.toFloat() / (1024f * 1024f) }

    fun onBackButtonClick() {
        viewModelScope.launch { _uiEvents.emit(DownloadsUiEvent.NavigateBack) }
    }

    fun onDeleteAllClick() {
        _uiState.showDeleteDownloadsDialog()
    }

    fun onDeleteAllConfirm() {
        viewModelScope.launch {
            val result = runCatching {
                _uiState.hideDialog()
                check(deleteEpisodesCache()) { "Failed to clear directory with cache" }
            }

            if (result.isSuccess) {
                _uiEvents.sendMessage("Deleted successfully")
            } else {
                _uiEvents.sendMessage("Delete failed")
            }
        }
    }

    fun onDownloadDeleteConfirm(episode: Episode) {
        viewModelScope.launch {
            val result = runCatching {
                _uiState.hideDialog()
                check(deleteEpisodeCache(episode)) { "Failed to delete cached file" }
            }

            if (result.isSuccess) {
                _uiEvents.sendMessage("Deleted successfully")
            } else {
                _uiEvents.sendMessage("Delete failed")
            }
        }
    }

    fun onDialogDismiss() {
        _uiState.hideDialog()
    }

    fun onDownloadDeleteClick(episode: Episode) {
        _uiState.showDeleteDownloadDialog(episode)
    }

    private fun MutableStateFlow<DownloadsUiState>.showDeleteDownloadDialog(episode: Episode) {
        update { it.copy(deleteDialog = DownloadsUiState.Dialog.DeleteDownloadDialog(episode)) }
    }

    private fun MutableStateFlow<DownloadsUiState>.showDeleteDownloadsDialog() {
        update { it.copy(deleteDialog = DownloadsUiState.Dialog.DeleteAllDialog) }
    }

    private fun MutableStateFlow<DownloadsUiState>.hideDialog() {
        update { it.copy(deleteDialog = null) }
    }

    private fun MutableSharedFlow<DownloadsUiEvent>.sendMessage(message: String) {
        viewModelScope.launch { emit(DownloadsUiEvent.ShowMessage(message)) }
    }
}