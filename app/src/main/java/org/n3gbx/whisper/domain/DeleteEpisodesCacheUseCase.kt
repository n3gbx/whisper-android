package org.n3gbx.whisper.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.n3gbx.whisper.core.common.GetEpisodesCacheDir
import org.n3gbx.whisper.data.EpisodeRepository
import javax.inject.Inject

class DeleteEpisodesCacheUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val getEpisodesCacheDir: GetEpisodesCacheDir,
) {

    suspend operator fun invoke(): Boolean {
        val isDeleted = withContext(Dispatchers.IO) { getEpisodesCacheDir().deleteRecursively() }
        if (!isDeleted) return false

        episodeRepository.clearAllEpisodesLocalPaths()
        return true
    }
}