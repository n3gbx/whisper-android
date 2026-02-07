package org.n3gbx.whisper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.n3gbx.whisper.database.entity.BookEmbeddedEntity
import org.n3gbx.whisper.database.entity.BookEntity

@Dao
interface BookDao {

    @Query("SELECT * FROM book")
    fun getAllBooks(): Flow<List<BookEmbeddedEntity>>

    @Query("SELECT * FROM book WHERE id_externalId = :externalId")
    fun getBookByExternalId(externalId: String): Flow<BookEmbeddedEntity?>

    @Query("SELECT * FROM book WHERE id_localId = :localId")
    fun getBookById(localId: String): Flow<BookEmbeddedEntity?>

    @Update(entity = BookEntity::class)
    suspend fun update(book: BookEntity.Update)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooks(books: List<BookEntity>)
}