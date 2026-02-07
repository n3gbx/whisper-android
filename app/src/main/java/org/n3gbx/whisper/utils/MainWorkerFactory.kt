package org.n3gbx.whisper.utils

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.platform.EpisodeDownloadWorker
import javax.inject.Inject

class MainWorkerFactory @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            EpisodeDownloadWorker::class.java.name ->
                EpisodeDownloadWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    episodeRepository = episodeRepository,
                )
            else -> null
        }
    }
}