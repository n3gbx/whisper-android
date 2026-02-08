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
import timber.log.Timber

class EpisodeDurationProbeWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val episodeDao: EpisodeDao,
    private val episodeDurationProberContext: EpisodeDurationProberContext,
): CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TIMEOUT_MS = 25_000L
        private const val LOG_TAG = "EpisodeDurationProbeWorker"
    }

    override suspend fun doWork(): Result {
        Timber.tag(LOG_TAG).d("Started")

        val episodes = episodeDao.getEpisodesWithoutDuration(limit = 10).first()

        val proberContext = episodeDurationProberContext
            .setStrategy(EpisodeDurationProberType.MEDIA_METADATA_RETRIEVER)

        Timber.tag(LOG_TAG).d("To probe: %s", episodes.size)

        for (episode in episodes) {
            val localId = episode.episode.localId

            if (isStopped) {
                Timber.tag(LOG_TAG).d("Stopped")
                return Result.success()
            }

            val duration = withTimeoutOrNull(TIMEOUT_MS) {
                proberContext.executeStrategy(episode.episode.url)
            }

            Timber.tag(LOG_TAG).d("Probed: localId=%s, duration=%s", localId, duration)

            if (duration != null && duration != UNSET_TIME) {
                episodeDao.setEpisodeDuration(
                    episodeLocalId = episode.episode.localId,
                    duration = duration,
                )
                Timber.tag(LOG_TAG).d("Updated: localId=%s, duration=%s", localId, duration)
            }
        }

        Timber.tag(LOG_TAG).d("Finished")
        return Result.success()
    }
}