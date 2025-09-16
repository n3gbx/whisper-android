package org.n3gbx.whisper.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.firstOrNull
import org.n3gbx.whisper.database.MainDatabase
import org.n3gbx.whisper.database.dao.BookmarkDao
import org.n3gbx.whisper.database.entity.BookmarkEntity
import javax.inject.Inject

class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val database: MainDatabase
) {

    suspend fun changeBookmark(bookId: String) {
        database.withTransaction {
            val bookmark = bookmarkDao.getByBookId(bookId).firstOrNull()
            if (bookmark == null) {
                bookmarkDao.insert(BookmarkEntity(bookId = bookId))
            } else {
                bookmarkDao.delete(bookmark)
            }
        }
    }
}