package org.n3gbx.whisper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.n3gbx.whisper.database.entity.BookEpisodePlaybackCacheEntity

@Dao
interface BookEpisodePlaybackCacheDao {

    @Query("SELECT * FROM book_episode_playback_cache WHERE bookId = :bookId AND episodeId = :episodeId")
    fun getByBookIdAndEpisodeId(bookId: String, episodeId: String): Flow<BookEpisodePlaybackCacheEntity?>

    @Query("SELECT * FROM book_episode_playback_cache WHERE bookId IN (:bookIds)")
    fun getAllByBookIds(bookIds: List<String>): Flow<List<BookEpisodePlaybackCacheEntity>>

    @Query("SELECT * FROM book_episode_playback_cache")
    fun getAll(): Flow<List<BookEpisodePlaybackCacheEntity>>

    @Query("SELECT * FROM book_episode_playback_cache WHERE bookId = :bookId")
    fun getAllByBookId(bookId: String): Flow<List<BookEpisodePlaybackCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookEpisodePlaybackCacheEntity: BookEpisodePlaybackCacheEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(bookEpisodePlaybackCacheEntity: BookEpisodePlaybackCacheEntity)
}