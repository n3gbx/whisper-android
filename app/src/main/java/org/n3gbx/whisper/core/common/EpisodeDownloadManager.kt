package org.n3gbx.whisper.core.common

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker.Companion.BOOK_LOCAL_ID_INPUT_DATA
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker.Companion.EPISODE_LOCAL_ID_INPUT_DATA
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker.Companion.EPISODE_URL_INPUT_DATA
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun enqueueDownload(
        bookLocalId: String,
        episodeLocalId: String,
        episodeUrl: String,
    ): UUID {
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

        return request.id
    }

    fun cancelDownload(workId: String) {
        WorkManager.getInstance(context).cancelWorkById(UUID.fromString(workId))
    }
}