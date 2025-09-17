package org.n3gbx.whisper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.n3gbx.whisper.database.entity.BookEmbeddedEntity
import org.n3gbx.whisper.database.entity.BookEntity
import org.n3gbx.whisper.database.entity.BookEpisodeEntity
import org.n3gbx.whisper.database.entity.BookEpisodeProgressEntity

@Dao
interface BookDao {

    @Query("SELECT * FROM book")
    fun getAllBooks(): Flow<List<BookEmbeddedEntity>>

    @Query("SELECT * FROM book WHERE externalId = :externalId")
    fun getBookByExternalId(externalId: String): Flow<BookEmbeddedEntity?>

    @Query("SELECT * FROM book WHERE id = :id")
    fun getBookById(id: String): Flow<BookEmbeddedEntity?>

    @Query("SELECT * FROM book_episode WHERE externalBookId = :externalBookId AND externalId = :externalBookEpisodeId")
    fun getBookEpisode(externalBookId: String, externalBookEpisodeId: String): Flow<BookEpisodeEntity?>

    @Update(entity = BookEntity::class)
    suspend fun update(book: BookEntity.Update)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookEpisodes(episodes: List<BookEpisodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookEpisodeProgresses(progresses: List<BookEpisodeProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookEpisodeProgress(progress: BookEpisodeProgressEntity)
}