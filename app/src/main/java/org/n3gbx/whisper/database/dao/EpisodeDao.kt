package org.n3gbx.whisper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.n3gbx.whisper.database.entity.EpisodeEmbeddedEntity
import org.n3gbx.whisper.database.entity.EpisodeEntity

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episode WHERE bookId_externalId = :bookExternalId AND id_externalId = :episodeExternalId")
    fun getEpisode(bookExternalId: String, episodeExternalId: String): Flow<EpisodeEmbeddedEntity?>

    @Query("SELECT * FROM episode WHERE id_localId = :localId")
    fun getEpisode(localId: String): Flow<EpisodeEmbeddedEntity?>

    @Query("SELECT * FROM episode WHERE localPath IS NOT NULL")
    fun getDownloadedEpisodes(): Flow<List<EpisodeEmbeddedEntity>>

    @Query("UPDATE episode SET localPath = NULL WHERE id_localId = :episodeLocalId")
    suspend fun clearEpisodeLocalPath(episodeLocalId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Query("UPDATE episode SET localPath=:localPath WHERE bookId_localId = :bookLocalId AND id_localId = :episodeLocalId")
    suspend fun updateEpisodeLocalPath(bookLocalId: String, episodeLocalId: String, localPath: String)
}