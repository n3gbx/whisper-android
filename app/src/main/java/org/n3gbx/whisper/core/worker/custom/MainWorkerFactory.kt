package org.n3gbx.whisper.core.worker.custom

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.n3gbx.whisper.core.common.EpisodeDurationProberContext
import org.n3gbx.whisper.core.common.GetEpisodesCacheDir
import org.n3gbx.whisper.core.worker.EpisodeDownloadWorker
import org.n3gbx.whisper.core.worker.EpisodeDurationProbeWorker
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.database.dao.EpisodeDao
import javax.inject.Inject

class MainWorkerFactory @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val episodeDao: EpisodeDao,
    private val episodeDurationProberContext: EpisodeDurationProberContext,
    private val getEpisodesCacheDir: GetEpisodesCacheDir,
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
                    getEpisodesCacheDir = getEpisodesCacheDir,
                )
            EpisodeDurationProbeWorker::class.java.name ->
                EpisodeDurationProbeWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    episodeDao = episodeDao,
                    episodeDurationProberContext = episodeDurationProberContext,
                )
            else -> null
        }
    }
}