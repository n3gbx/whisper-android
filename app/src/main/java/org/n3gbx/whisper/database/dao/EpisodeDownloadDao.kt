package org.n3gbx.whisper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.n3gbx.whisper.database.entity.EpisodeDownloadEntity
import org.n3gbx.whisper.model.DownloadState

@Dao
interface EpisodeDownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(download: EpisodeDownloadEntity)

    @Query("UPDATE episode_download SET progress = :progress, state = :state WHERE episodeId_localId = :episodeLocalId")
    suspend fun updateProgress(episodeLocalId: String, progress: Int, state: DownloadState)

    @Query("UPDATE episode_download SET state = :state WHERE episodeId_localId = :episodeLocalId")
    suspend fun updateState(episodeLocalId: String, state: DownloadState)

    @Query("DELETE FROM episode_download WHERE episodeId_localId = :episodeLocalId ")
    suspend fun deleteDownload(episodeLocalId: String)
}