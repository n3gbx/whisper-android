package org.n3gbx.whisper.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.model.Episode
import java.io.File
import javax.inject.Inject

class DeleteEpisodeCacheUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {

    suspend operator fun invoke(episode: Episode): Boolean {
        val file = episode.localPath?.let { File(it) } ?: return true
        val isDeleted = withContext(Dispatchers.IO) { file.delete() }
        if (!isDeleted) return false

        episodeRepository.clearEpisodeLocalPath(episode.id.localId)
        return true
    }
}