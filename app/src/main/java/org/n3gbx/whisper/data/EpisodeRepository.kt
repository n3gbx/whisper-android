package org.n3gbx.whisper.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.n3gbx.whisper.database.MainDatabase
import org.n3gbx.whisper.database.dao.EpisodeDao
import org.n3gbx.whisper.database.dao.EpisodeDownloadDao
import org.n3gbx.whisper.database.dao.EpisodeProgressDao
import org.n3gbx.whisper.database.entity.EpisodeDownloadEntity
import org.n3gbx.whisper.database.entity.EpisodeProgressEntity
import org.n3gbx.whisper.model.DownloadState
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeRepository @Inject constructor(
    private val episodeProgressDao: EpisodeProgressDao,
    private val episodeDownloadDao: EpisodeDownloadDao,
    private val episodeDao: EpisodeDao,
    private val database: MainDatabase,
) {

    suspend fun updateEpisodeProgress(
        bookExternalId: String,
        episodeExternalId: String,
        currentTime: Long
    ) {
        database.withTransaction {
            val episode = episodeDao.getEpisode(bookExternalId, episodeExternalId).first()!!

            val entity = EpisodeProgressEntity(
                episodeLocalId = episode.episode.localId,
                episodeExternalId = episode.episode.externalId,
                time = currentTime,
                lastUpdatedAt = LocalDateTime.now()
            )
            episodeProgressDao.upsertProgress(entity)
        }
    }

    suspend fun markEpisodeDownloadQueued(
        workId: String,
        episodeLocalId: String,
    ) {
        database.withTransaction {
            val episode = episodeDao.getEpisode(localId = episodeLocalId).first()!!

            episodeDownloadDao.upsertDownload(
                download = EpisodeDownloadEntity(
                    episodeLocalId = episode.episode.localId,
                    episodeExternalId = episode.episode.externalId,
                    workId = workId,
                    progress = 0,
                    state = DownloadState.QUEUED,
                )
            )
        }
    }

    suspend fun getEpisodeDownloadWorkId(episodeLocalId: String) =
        episodeDao.getEpisode(episodeLocalId).first()?.episodeDownload?.workId

    suspend fun markEpisodeDownloadProgressing(episodeLocalId: String, progress: Int) {
        episodeDownloadDao.updateProgress(
            episodeLocalId = episodeLocalId,
            progress = progress,
            state = DownloadState.PROGRESSING,
        )
    }

    suspend fun markEpisodeDownloadFailed(episodeLocalId: String) {
        episodeDownloadDao.updateProgress(
            episodeLocalId = episodeLocalId,
            progress = 0,
            state = DownloadState.FAILED,
        )
    }

    suspend fun clearEpisodeDownload(episodeLocalId: String) {
        episodeDownloadDao.deleteDownload(episodeLocalId)
    }

    suspend fun markEpisodeDownloadCompleted(
        bookLocalId: String,
        episodeLocalId: String,
        localPath: String,
    ) {
        database.withTransaction {
            episodeDao.updateEpisodeLocalPath(
                bookLocalId = bookLocalId,
                episodeLocalId = episodeLocalId,
                localPath = localPath
            )
            episodeDownloadDao.deleteDownload(
                episodeLocalId = episodeLocalId
            )
        }
    }

    fun getDownloadedEpisodes() =
        episodeDao.getDownloadedEpisodes()

    suspend fun clearEpisodeLocalPath(episodeLocalId: String) =
        episodeDao.clearEpisodeLocalPath(episodeLocalId)
}