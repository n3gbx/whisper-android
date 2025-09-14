package org.n3gbx.whisper.data

import androidx.room.withTransaction
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import org.n3gbx.whisper.data.dto.BookDto
import org.n3gbx.whisper.data.dto.BookEpisodeDto
import org.n3gbx.whisper.database.MainDatabase
import org.n3gbx.whisper.database.dao.BookEpisodePlaybackCacheDao
import org.n3gbx.whisper.database.dao.BookmarkDao
import org.n3gbx.whisper.database.entity.BookEpisodePlaybackCacheEntity
import org.n3gbx.whisper.database.entity.BookmarkEntity
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.BookEpisode
import org.n3gbx.whisper.model.BookEpisodePlaybackCache
import org.n3gbx.whisper.utils.snapshotFlow
import java.time.LocalDateTime
import javax.inject.Inject

class BookRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: MainDatabase,
    private val bookmarkDao: BookmarkDao,
    private val bookEpisodePlaybackCacheDao: BookEpisodePlaybackCacheDao
) {
    private val booksCollection = "books"

    fun getCachedBooks(): Flow<List<Book>> {
        return bookEpisodePlaybackCacheDao.getAll().flatMapConcat { bookEpisodesPlaybackCaches ->
            val bookIds = bookEpisodesPlaybackCaches.map { it.bookId }

            val booksFlow = getFirestoreBooks(bookIds)
            val bookmarksFlow = bookmarkDao.getAllByBookIds(bookIds)

            combine(
                booksFlow,
                bookmarksFlow
            ) { firestoreBooks, bookmarks ->
                firestoreBooks.mapNotNull { firestoreBook ->
                    val bookmark = bookmarks.findBookmarkByBookId(firestoreBook.id)

                    firestoreBook.mapToBook(
                        bookmark = bookmark,
                        bookEpisodesPlaybackCaches = bookEpisodesPlaybackCaches
                    )
                }
            }
        }
    }

    fun getBook(id: String): Flow<Book?> {
        return combine(
            getFirestoreBook(id),
            bookmarkDao.getByBookId(id),
            bookEpisodePlaybackCacheDao.getAllByBookId(id)
        ) { firestoreBook, bookmark, bookEpisodesPlaybackCaches ->
            Triple(firestoreBook, bookmark, bookEpisodesPlaybackCaches)
        }.map { (firestoreBook, bookmark, bookEpisodesPlaybackCaches) ->
            firestoreBook?.let {
                firestoreBook.mapToBook(
                    bookmark = bookmark,
                    bookEpisodesPlaybackCaches = bookEpisodesPlaybackCaches
                )
            }
        }
    }

    fun getBooks(): Flow<List<Book>> {
        return combine(
            getFirestoreBooks(),
            bookmarkDao.getAll(),
        ) { firestoreBooks, bookmarks ->
            Pair(firestoreBooks, bookmarks)
        }.map { (firestoreBooks, bookmarks) ->
            firestoreBooks.mapNotNull { firestoreBook ->
                val bookmark = bookmarks.findBookmarkByBookId(firestoreBook.id)

                firestoreBook.mapToBook(
                    bookmark = bookmark,
                    bookEpisodesPlaybackCaches = emptyList()
                )
            }
        }
    }

    suspend fun saveBookEpisodePlayback(
        bookId: String,
        episodeId: String,
        durationTime: Long,
        currentTime: Long
    ) {
        database.withTransaction {
            val newEntity = BookEpisodePlaybackCacheEntity(
                bookId = bookId,
                episodeId = episodeId,
                durationTime = durationTime,
                lastTime = currentTime,
                lastUpdatedAt = LocalDateTime.now()
            )
            bookEpisodePlaybackCacheDao.insert(newEntity)
        }
    }

    private fun BookDto.mapToBook(
        bookmark: BookmarkEntity?,
        bookEpisodesPlaybackCaches: List<BookEpisodePlaybackCacheEntity> = emptyList()
    ): Book? {
        val isBookmarked = bookmark != null
        val episodes = episodes.mapToBookEpisodes(bookEpisodesPlaybackCaches)
        val currentEpisode = episodes
            .sortedByDescending { it.playbackCache?.lastUpdatedAt }
            .firstOrNull { it.playbackCache?.isFinished?.not() ?: true }

        return if (episodes.isNotEmpty() && currentEpisode != null) {
            Book(
                id = id,
                title = title,
                author = author,
                narrator = narrator,
                coverUrl = coverUrl,
                description = description,
                isBookmarked = isBookmarked,
                currentEpisode = currentEpisode,
                episodes = episodes
            )
        } else {
            null
        }
    }

    private fun List<BookEpisodeDto>.mapToBookEpisodes(
        playbackCaches: List<BookEpisodePlaybackCacheEntity> = emptyList()
    ) = map { bookEpisode ->
        val playbackCache =
            playbackCaches.firstOrNull { it.episodeId == bookEpisode.episodeId }?.let {
                BookEpisodePlaybackCache(
                    durationTime = it.durationTime,
                    lastTime = it.lastTime,
                    lastUpdatedAt = it.lastUpdatedAt
                )
            }

        BookEpisode(
            id = bookEpisode.episodeId,
            url = bookEpisode.episodeUrl,
            playbackCache = playbackCache
        )
    }

    private fun List<BookmarkEntity>.findBookmarkByBookId(bookId: String) =
        firstOrNull { it.bookId == bookId }

    private fun getFirestoreBook(id: String) =
        firestore.collection(booksCollection)
            .whereEqualTo(FieldPath.documentId(), id)
            .snapshotFlow()
            .map { querySnapshot ->
                querySnapshot.documents.getOrNull(0)?.let { document ->
                    document.toObject<BookDto>()?.copy(id = document.id)
                }
            }

    private fun getFirestoreBooks(ids: List<String>) =
        firestore.collection(booksCollection)
            .whereIn(FieldPath.documentId(), ids)
            .snapshotFlow()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    document.toObject<BookDto>()?.copy(id = document.id)
                }
            }

    private fun getFirestoreBooks() =
        firestore.collection(booksCollection)
            .snapshotFlow()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    document.toObject<BookDto>()?.copy(id = document.id)
                }
            }
}