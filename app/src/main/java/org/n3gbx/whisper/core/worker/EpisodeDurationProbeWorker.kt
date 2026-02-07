package org.n3gbx.whisper.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.n3gbx.whisper.core.Constants.UNSET_TIME
import org.n3gbx.whisper.core.common.EpisodeDurationProberContext
import org.n3gbx.whisper.core.common.EpisodeDurationProberType
import org.n3gbx.whisper.database.dao.EpisodeDao

class EpisodeDurationProbeWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val episodeDao: EpisodeDao,
    private val episodeDurationProberContext: EpisodeDurationProberContext,
): CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TIMEOUT_MS = 25_000L
    }

    override suspend fun doWork(): Result {
        val episodes = episodeDao.getEpisodesWithoutDuration(limit = 10).first()
        val proberContext = episodeDurationProberContext
            .setStrategy(EpisodeDurationProberType.MEDIA_METADATA_RETRIEVER)

        for (episode in episodes) {
            if (isStopped) {
                return Result.success()
            }

            val duration = withTimeoutOrNull(TIMEOUT_MS) {
                proberContext.executeStrategy(episode.episode.url)
            }

            if (duration != null && duration != UNSET_TIME) {
                episodeDao.setEpisodeDuration(
                    episodeLocalId = episode.episode.localId,
                    duration = duration,
                )
            }
        }

        return Result.success()
    }
}