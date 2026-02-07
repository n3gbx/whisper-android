package org.n3gbx.whisper.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.n3gbx.whisper.data.EpisodeRepository
import java.io.File


class EpisodeDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val episodeRepository: EpisodeRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val episodeUrl = inputData.getString(EPISODE_URL_INPUT_DATA) ?: return Result.failure()
        val episodeLocalId = inputData.getString(EPISODE_LOCAL_ID_INPUT_DATA) ?: return Result.failure()
        val bookLocalId = inputData.getString(BOOK_LOCAL_ID_INPUT_DATA) ?: return Result.failure()

        val episodesDir = File(
            applicationContext.externalCacheDir,
            "episodes",
        )

        if (!episodesDir.exists()) {
            episodesDir.mkdirs()
        }

        val file = File(episodesDir, "$episodeLocalId.mp3")

        try {
            episodeRepository.markEpisodeDownloadProgressing(
                episodeLocalId = episodeLocalId,
                progress = 0,
            )

            val client = OkHttpClient()
            val request = Request.Builder().url(episodeUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                episodeRepository.markEpisodeDownloadFailed(episodeLocalId)
                return Result.retry()
            }

            val body = response.body ?: run {
                episodeRepository.markEpisodeDownloadFailed(episodeLocalId)
                return Result.retry()
            }

            val total = body.contentLength()
            val input = body.byteStream()
            val output = file.outputStream()

            var downloaded = 0L
            val buffer = ByteArray(8_192)
            var lastProgress = 0

            withContext(Dispatchers.IO) {
                while (true) {
                    if (isStopped) {
                        input.close()
                        output.close()
                        file.delete()
                        episodeRepository.clearEpisodeDownload(episodeLocalId)
                        return@withContext Result.failure()
                    }

                    val read = input.read(buffer)
                    if (read == -1) break

                    output.write(buffer, 0, read)
                    downloaded += read

                    if (total > 0) {
                        val progress = ((downloaded * 100) / total).toInt()
                        setProgress(workDataOf(PROGRESS_WORK_DATA to progress))

                        if (progress != lastProgress) {
                            lastProgress = progress
                            episodeRepository.markEpisodeDownloadProgressing(
                                episodeLocalId = episodeLocalId,
                                progress = progress,
                            )
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()
            }

            episodeRepository.markEpisodeDownloadCompleted(
                bookLocalId = bookLocalId,
                episodeLocalId = episodeLocalId,
                localPath = file.absolutePath,
            )

            return Result.success()
        } catch (e: Exception) {
            file.delete()
            episodeRepository.markEpisodeDownloadFailed(episodeLocalId)
            delay(2000)
            episodeRepository.clearEpisodeDownload(episodeLocalId)
            return Result.failure()
        }
    }

    companion object {
        const val EPISODE_URL_INPUT_DATA = "episodeUrl"
        const val EPISODE_LOCAL_ID_INPUT_DATA = "episodeLocalId"
        const val BOOK_LOCAL_ID_INPUT_DATA = "bookLocalId"
        const val PROGRESS_WORK_DATA = "progress"
    }
}