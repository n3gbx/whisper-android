package org.n3gbx.whisper.core.common

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import org.n3gbx.whisper.core.Constants.INVALID_SIZE
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker.Companion.BOOK_LOCAL_ID_INPUT_DATA
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker.Companion.EPISODE_LOCAL_ID_INPUT_DATA
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker.Companion.EPISODE_URL_INPUT_DATA
import org.n3gbx.whisper.domain.GetEpisodeFileSizeUseCase
import org.n3gbx.whisper.domain.GetEpisodesCacheDirSizeUseCase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getEpisodesCacheDirSizeLimit: GetEpisodesCacheDirSizeLimit,
    private val getEpisodeFileSize: GetEpisodeFileSizeUseCase,
    private val getEpisodesCacheDirSize: GetEpisodesCacheDirSizeUseCase,
) {

    suspend fun enqueueDownload(
        bookLocalId: String,
        episodeLocalId: String,
        episodeUrl: String,
    ): Result {
        if (!isDownloadable(episodeUrl)) {
            return Result.SizeLimit
        }

        val data = workDataOf(
            EPISODE_URL_INPUT_DATA to episodeUrl,
            EPISODE_LOCAL_ID_INPUT_DATA to episodeLocalId,
            BOOK_LOCAL_ID_INPUT_DATA to bookLocalId,
        )

        val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)

        return Result.Enqueued(request.id)
    }

    fun cancelDownload(workId: String) {
        WorkManager.getInstance(context).cancelWorkById(UUID.fromString(workId))
    }

    private suspend fun isDownloadable(episodeUrl: String): Boolean {
        val cacheDirSizeLimitBytes = getEpisodesCacheDirSizeLimit()
        val episodeFileSizeBytes = getEpisodeFileSize(episodeUrl)
        val episodesCacheDirSizeBytes = getEpisodesCacheDirSize()

        if (episodeFileSizeBytes == INVALID_SIZE) return false

        return (cacheDirSizeLimitBytes - episodesCacheDirSizeBytes - episodeFileSizeBytes) >= 0
    }

    sealed interface Result {
        data class Enqueued(val uuid: UUID) : Result
        data object SizeLimit : Result
    }
}