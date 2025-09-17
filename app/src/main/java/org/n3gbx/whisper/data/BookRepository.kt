package org.n3gbx.whisper.data

import android.media.MediaMetadataRetriever
import androidx.room.withTransaction
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.n3gbx.whisper.Constants.UNSET_TIME
import org.n3gbx.whisper.data.common.NetworkBoundResource
import org.n3gbx.whisper.data.dto.BookDto
import org.n3gbx.whisper.database.MainDatabase
import org.n3gbx.whisper.database.dao.BookDao
import org.n3gbx.whisper.database.entity.BookEmbeddedEntity
import org.n3gbx.whisper.database.entity.BookEntity
import org.n3gbx.whisper.database.entity.BookEpisodeEmbeddedEntity
import org.n3gbx.whisper.database.entity.BookEpisodeEntity
import org.n3gbx.whisper.database.entity.BookEpisodeProgressEntity
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.BookEpisode
import org.n3gbx.whisper.model.BookEpisodeProgress
import org.n3gbx.whisper.model.BooksType
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Singleton
class BookRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: MainDatabase,
    private val bookDao: BookDao,
    private val networkBoundResource: NetworkBoundResource
) {
    private val booksCollection = "books"

    fun getBook(id: Identifier): Flow<Result<Book?>> {
        return networkBoundResource(
            query = {
                bookDao.getBookById(id.localId).mapNotNull { it?.mapToModel() }
            },
            fetch = {
                getFirestoreBook(id.externalId)
            },
            saveFetched = { remoteBook, localBook ->
                val remoteToLocalBook = remoteBook to localBook
                val entity = remoteToLocalBook.mapToEmbeddedEntity()

                if (entity != null) {
                    database.withTransaction {
                        val bookEntity = entity.book
                        val episodeEntities = entity.episodeEntities()
                        val episodeProgressEntities = entity.episodeProgressEntities()

                        bookDao.insertBook(bookEntity)
                        bookDao.insertBookEpisodes(episodeEntities)
                        bookDao.insertBookEpisodeProgresses(episodeProgressEntities)
                    }
                }
            },
            shouldFetch = { localBooks ->
                localBooks == null
            }
        )
    }

    fun getBooks(
        query: String? = null,
        shouldRefresh: Boolean = false,
        booksType: BooksType? = null
    ): Flow<Result<List<Book>>> {
        return networkBoundResource(
            query = {
                bookDao.getAllBooks().map { it.mapToModels() }
            },
            fetch = {
                getFirestoreBooks()
            },
            saveFetched = { remoteBooks, localBooks ->
                val remoteToLocalBookMap = associateRemoteAndLocalBooks(remoteBooks, localBooks)

                database.withTransaction {
                    val entities = remoteToLocalBookMap.mapToEmbeddedEntities()
                    val bookEntities = entities.bookEntities()
                    val episodeEntities = entities.episodeEntities()
                    val episodeProgressEntities = entities.episodeProgressEntities()

                    bookDao.insertBooks(bookEntities)
                    bookDao.insertBookEpisodes(episodeEntities)
                    bookDao.insertBookEpisodeProgresses(episodeProgressEntities)
                }
            },
            shouldFetch = { localBooks ->
                localBooks.isNullOrEmpty() || shouldRefresh
            }
        ).filterBooks(query, booksType)
    }

    suspend fun updateBookEpisodeProgress(
        externalBookId: String,
        externalEpisodeId: String,
        currentTime: Long
    ) {
        database.withTransaction {
            val episode = bookDao.getBookEpisode(externalBookId, externalEpisodeId).first()!!

            val entity = BookEpisodeProgressEntity(
                episodeId = episode.id,
                externalEpisodeId = externalEpisodeId,
                lastTime = currentTime,
                lastUpdatedAt = LocalDateTime.now()
            )
            bookDao.insertBookEpisodeProgress(entity)
        }
    }

    suspend fun updateBookBookmark(
        bookId: Identifier,
    ) {
        database.withTransaction {
            val isBookmarked = bookDao.getBookById(bookId.localId).first()!!.book.isBookmarked
            val newIsBookmarked = !isBookmarked

            bookDao.update(
                BookEntity.Update(
                    id = bookId.localId,
                    isBookmarked = newIsBookmarked
                )
            )
        }
    }

    private fun Flow<Result<List<Book>>>.filterBooks(
        searchQuery: String?,
        booksType: BooksType?
    ) = transform { value ->
        val result = if (value is Result.Success) {
            val filteredData = value.data.filter { book ->
                val isBooksTypeMatched = when (booksType) {
                    BooksType.STARTED -> book.isStarted
                    BooksType.FINISHED -> book.isFinished
                    BooksType.SAVED -> book.isBookmarked
                    else -> true
                }
                book.matchesQuery(searchQuery) && isBooksTypeMatched
            }
            Result.Success(filteredData)
        } else {
            value
        }
        return@transform emit(result)
    }

    private fun associateRemoteAndLocalBooks(
        remoteBooks: List<BookDto>,
        localBooks: List<Book>?
    ): Map<BookDto, Book?> {
        val localBookByExternalId = localBooks?.associateBy { it.id.externalId } ?: emptyMap()

        return remoteBooks.associateWith { remote ->
            localBookByExternalId[remote.id]
        }
    }

    private suspend fun Map<BookDto, Book?>.mapToEmbeddedEntities(): List<BookEmbeddedEntity> {
        val defaultLocalDateTime = LocalDateTime.now()

        return mapNotNull { (bookDto, book) ->
            (bookDto to book).mapToEmbeddedEntity(defaultLocalDateTime)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun Pair<BookDto?, Book?>.mapToEmbeddedEntity(
        defaultLocalDateTime: LocalDateTime = LocalDateTime.now()
    ): BookEmbeddedEntity? {
        val (bookDto, book) = this

        if (bookDto == null) return null

        val externalBookId = bookDto.id
        val localBookId = book?.id?.localId ?: Uuid.random().toString()

        val episodes = bookDto.episodes.map { episodeDto ->
            val episode = book?.findEpisodeByTitle(episodeDto.episodeTitle)
            val localEpisodeId = episode?.id?.localId ?: Uuid.random().toString()
            val episodeDuration = episode?.duration ?: UNSET_TIME

            val bookEpisodeProgressEntity = BookEpisodeProgressEntity(
                id = episode?.progress?.id ?: Uuid.random().toString(),
                episodeId = episode?.progress?.episodeId?.localId ?: localEpisodeId,
                externalEpisodeId = episode?.progress?.episodeId?.externalId ?: episodeDto.episodeTitle,
                lastTime = episode?.progress?.lastTime ?: 0,
                lastUpdatedAt = episode?.progress?.lastUpdatedAt ?: defaultLocalDateTime
            )

            BookEpisodeEmbeddedEntity(
                bookEpisode = BookEpisodeEntity(
                    id = localEpisodeId,
                    bookId = localBookId,
                    externalBookId = externalBookId,
                    externalId = episodeDto.episodeTitle,
                    title = episodeDto.episodeTitle,
                    url = episodeDto.episodeUrl,
                    duration = episodeDuration,
                ),
                bookEpisodeProgress = bookEpisodeProgressEntity
            )
        }.withDurations()

        return if (episodes.isNotEmpty()) {
            BookEmbeddedEntity(
                book = BookEntity(
                    id = localBookId,
                    externalId = externalBookId,
                    title = bookDto.title,
                    author = bookDto.author,
                    narrator = bookDto.narrator,
                    coverUrl = bookDto.coverUrl,
                    description = bookDto.description,
                    isBookmarked = book?.isBookmarked ?: false
                ),
                episodes = episodes
            )
        } else {
            null
        }
    }

    private fun Book.findEpisodeByTitle(title: String) = episodes.find { it.title == title }

    private fun List<BookEmbeddedEntity>.mapToModels(): List<Book> {
        return map { entity -> entity.mapToModel() }
    }

    private fun BookEmbeddedEntity.mapToModel(): Book {
        val episodes = episodes.mapToModel()
        val recentEpisode = episodes
            .sortedByDescending { it.progress.lastUpdatedAt }
            .firstOrNull { it.isFinished.not() }
            ?: episodes[0]

        return Book(
            id = Identifier(book.id, book.externalId),
            title = book.title,
            author = book.author,
            narrator = book.narrator,
            coverUrl = book.coverUrl,
            description = book.description,
            isBookmarked = book.isBookmarked,
            recentEpisode = recentEpisode,
            episodes = episodes
        )
    }

    private fun List<BookEpisodeEmbeddedEntity>.mapToModel(): List<BookEpisode> {
        return map { entity ->
            BookEpisode(
                id = Identifier(entity.bookEpisode.id, entity.bookEpisode.externalId),
                bookId = Identifier(entity.bookEpisode.bookId, entity.bookEpisode.externalBookId),
                title = entity.bookEpisode.title,
                url = entity.bookEpisode.url,
                duration = entity.bookEpisode.duration,
                progress = entity.bookEpisodeProgress.mapToModel()
            )
        }
    }

    private fun BookEpisodeProgressEntity.mapToModel(): BookEpisodeProgress {
        return BookEpisodeProgress(
            id = id,
            episodeId = Identifier(episodeId, externalEpisodeId),
            lastTime = lastTime,
            lastUpdatedAt = lastUpdatedAt
        )
    }

    private suspend fun List<BookEpisodeEmbeddedEntity>.withDurations() = coroutineScope {
        map { entity ->
            withContext(Dispatchers.IO) {
                async {
                    if (entity.bookEpisode.duration == UNSET_TIME) {
                        val duration = entity.getMetadataDurationBlocking() ?: UNSET_TIME

                        entity.copy(
                            bookEpisode = entity.bookEpisode.copy(
                                duration = duration
                            )
                        )
                    } else {
                        entity
                    }
                }
            }
        }.awaitAll()
    }

    private fun BookEpisodeEmbeddedEntity.getMetadataDurationBlocking(): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(bookEpisode.url, HashMap())
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    private fun List<BookEmbeddedEntity>.bookEntities() = map { it.book }

    private fun List<BookEmbeddedEntity>.episodeEntities() = flatMap { it.episodes.map { e -> e.bookEpisode } }

    private fun List<BookEmbeddedEntity>.episodeProgressEntities() = flatMap { it.episodes.map { e -> e.bookEpisodeProgress } }

    private fun BookEmbeddedEntity.episodeEntities() = episodes.map { it.bookEpisode }

    private fun BookEmbeddedEntity.episodeProgressEntities() = episodes.map { it.bookEpisodeProgress }

    private suspend fun getFirestoreBook(id: String) =
        firestore.collection(booksCollection)
            .whereEqualTo(FieldPath.documentId(), id)
            .get()
            .await()
            .documents
            .let { documents ->
                documents.getOrNull(0)?.let { document ->
                    document.toObject<BookDto>()?.copy(id = document.id)
                }
            }

    private suspend fun getFirestoreBooks(): List<BookDto> {
        return firestore.collection(booksCollection)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject<BookDto>()?.copy(id = document.id)
            }
    }
}