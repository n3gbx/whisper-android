package org.n3gbx.whisper.feature.downloads

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.model.Episode

@Immutable
data class DownloadsUiState(
    val isLoading: Boolean = true,
    val episodes: List<Pair<Episode, EpisodeSize>> = emptyList(),
    val deleteDialog: Dialog? = null,
) {

    val isClearAllButtonVisible: Boolean get() = episodes.isNotEmpty()

    sealed interface Dialog {
        data class DeleteDownloadDialog(val episode: Episode) : Dialog
        data object DeleteAllDialog : Dialog
    }
}

sealed interface DownloadsUiEvent {
    data class ShowMessage(val message: String): DownloadsUiEvent
    data object NavigateBack: DownloadsUiEvent
}

typealias EpisodeSize = String