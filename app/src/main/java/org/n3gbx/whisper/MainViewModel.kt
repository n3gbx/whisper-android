package org.n3gbx.whisper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.EpisodeRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) : ViewModel() {

    fun reconcileDownloadedEpisodeFiles() {
        viewModelScope.launch {
            val episodes = episodeRepository.getDownloadedEpisodes().first()

            episodes.forEach { episode ->
                val localPath = episode.episode.localPath

                if (localPath != null) {
                    val file = File(localPath)
                    if (!file.exists()) {
                        episodeRepository.clearEpisodeLocalPath(episode.episode.localId)
                    }
                }
            }
        }
    }
}