package org.n3gbx.whisper.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.n3gbx.whisper.database.entity.BookmarkEntity

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmark WHERE bookId = :bookId")
    fun getByBookId(bookId: String): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmark")
    fun getAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark WHERE bookId IN (:bookIds)")
    fun getAllByBookIds(bookIds: List<String>): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookmarkEntity: BookmarkEntity)

    @Delete
    fun delete(bookmarkEntity: BookmarkEntity)

    @Query("DELETE FROM bookmark WHERE bookId = :bookId")
    fun deleteByBookId(bookId: String)
}