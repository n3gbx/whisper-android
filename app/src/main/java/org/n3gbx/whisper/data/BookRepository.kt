package org.n3gbx.whisper.data

import android.media.MediaMetadataRetriever
import androidx.media3.common.util.UnstableApi
import androidx.room.withTransaction
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.n3gbx.whisper.Constants.UNSET_TIME
import org.n3gbx.whisper.data.common.NetworkBoundResource
import org.n3gbx.whisper.data.dto.BookDto
import org.n3gbx.whisper.database.MainDatabase
import org.n3gbx.whisper.database.dao.BookDao
import org.n3gbx.whisper.database.dao.EpisodeDao
import org.n3gbx.whisper.database.dao.EpisodeDownloadDao
import org.n3gbx.whisper.database.dao.EpisodeProgressDao
import org.n3gbx.whisper.database.entity.BookEmbeddedEntity
import org.n3gbx.whisper.database.entity.BookEntity
import org.n3gbx.whisper.database.entity.EpisodeDownloadEntity
import org.n3gbx.whisper.database.entity.EpisodeEmbeddedEntity
import org.n3gbx.whisper.database.entity.EpisodeEntity
import org.n3gbx.whisper.database.entity.EpisodeProgressEntity
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.Episode
import org.n3gbx.whisper.model.EpisodeProgress
import org.n3gbx.whisper.model.BooksSortType
import org.n3gbx.whisper.model.BooksType
import org.n3gbx.whisper.model.DownloadState
import org.n3gbx.whisper.model.EpisodeDownload
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import org.n3gbx.whisper.utils.MetadataProbePlayer
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@androidx.annotation.OptIn(UnstableApi::class)
@Singleton
class BookRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: MainDatabase,
    private val bookDao: BookDao,
    private val episodeDao: EpisodeDao,
    private val episodeProgressDao: EpisodeProgressDao,
    private val episodeDownloadDao: EpisodeDownloadDao,
    private val networkBoundResource: NetworkBoundResource,
    private val metadataProbePlayer: MetadataProbePlayer,
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

                        bookDao.upsertBook(bookEntity)
                        episodeDao.upsertEpisodes(episodeEntities)
                        episodeProgressDao.upsertProgresses(episodeProgressEntities)
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
        booksType: BooksType? = null,
        booksSortType: BooksSortType? = null
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

                    bookDao.upsertBooks(bookEntities)
                    episodeDao.upsertEpisodes(episodeEntities)
                    episodeProgressDao.upsertProgresses(episodeProgressEntities)
                }
            },
            shouldFetch = { localBooks ->
                localBooks.isNullOrEmpty() || shouldRefresh
            }
        ).filterBooks(query, booksType, booksSortType)
    }

    suspend fun updateBookBookmark(
        bookId: Identifier,
    ) {
        database.withTransaction {
            val isBookmarked = bookDao.getBookById(bookId.localId).first()!!.book.isBookmarked
            val newIsBookmarked = !isBookmarked

            bookDao.update(
                BookEntity.Update(
                    localId = bookId.localId,
                    isBookmarked = newIsBookmarked
                )
            )
        }
    }

    private fun Flow<Result<List<Book>>>.filterBooks(
        searchQuery: String?,
        booksType: BooksType?,
        booksSortType: BooksSortType?
    ) = map { result ->
        if (result !is Result.Success) return@map result

        val filteredData = result.data.filter { book ->
            val isBooksTypeMatched = when (booksType) {
                BooksType.STARTED -> book.isStarted
                BooksType.FINISHED -> book.isFinished
                BooksType.SAVED -> book.isBookmarked
                else -> true
            }
            book.matchesQuery(searchQuery) && isBooksTypeMatched
        }

        val finalData = when (booksSortType) {
            BooksSortType.TITLE_ASC ->
                filteredData.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            BooksSortType.TITLE_DESC ->
                filteredData.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            BooksSortType.LENGTH_ASC ->
                filteredData.sortedBy { it.totalDuration }
            BooksSortType.LENGTH_DESC ->
                filteredData.sortedByDescending { it.totalDuration }
            else -> filteredData
        }

        Result.Success(finalData)
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

        val bookExternalId = bookDto.id
        val bookLocalId = book?.id?.localId ?: Uuid.random().toString()

        val episodes = bookDto.episodes.map { episodeDto ->
            val episodeExternalId = episodeDto.episodeTitle

            val episode = book?.findEpisodeByTitle(episodeExternalId)
            val episodeId = episode?.id?.localId ?: Uuid.random().toString()
            val episodeDuration = episode?.duration ?: UNSET_TIME

            val episodeProgressEntity = EpisodeProgressEntity(
                episodeLocalId = episode?.progress?.episodeId?.localId ?: episodeId,
                episodeExternalId = episode?.progress?.episodeId?.externalId ?: episodeExternalId,
                time = episode?.progress?.time ?: 0,
                lastUpdatedAt = episode?.progress?.lastUpdatedAt ?: defaultLocalDateTime,
            )

            val episodeDownloadEntity = EpisodeDownloadEntity(
                episodeLocalId = episode?.progress?.episodeId?.localId ?: episodeId,
                episodeExternalId = episode?.progress?.episodeId?.externalId ?: episodeExternalId,
                workId = episode?.download?.workId,
                progress = episode?.download?.progress ?: 0,
                state = episode?.download?.state ?: DownloadState.IDLE,
                lastUpdatedAt = episode?.download?.lastUpdatedAt ?: defaultLocalDateTime
            )

            EpisodeEmbeddedEntity(
                episode = EpisodeEntity(
                    localId = episodeId,
                    externalId = episodeExternalId,
                    bookLocalId = bookLocalId,
                    bookExternalId = bookExternalId,
                    title = episodeDto.episodeTitle,
                    url = episodeDto.episodeUrl,
                    duration = episodeDuration,
                    localPath = episode?.localPath,
                ),
                episodeProgress = episodeProgressEntity,
                episodeDownload = episodeDownloadEntity,
            )
        }.probeDurations()

        return if (episodes.isNotEmpty()) {
            BookEmbeddedEntity(
                book = BookEntity(
                    localId = bookLocalId,
                    externalId = bookExternalId,
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
        val episodes = episodes
            .mapToModel()

        val recentEpisode = episodes
            .sortedByDescending { it.progress.lastUpdatedAt }
            .firstOrNull { it.isFinished.not() }
            ?: episodes[0]

        return Book(
            id = Identifier(
                localId = book.localId,
                externalId = book.externalId,
            ),
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

    private fun List<EpisodeEmbeddedEntity>.mapToModel(): List<Episode> {
        return map { entity ->
            Episode(
                id = Identifier(
                    localId = entity.episode.localId,
                    externalId = entity.episode.externalId,
                ),
                bookId = Identifier(
                    localId = entity.episode.bookLocalId,
                    externalId = entity.episode.bookExternalId,
                ),
                title = entity.episode.title,
                url = entity.episode.url,
                duration = entity.episode.duration,
                progress = entity.episodeProgress.mapToModel(),
                download = entity.episodeDownload?.mapToModel(),
                localPath = entity.episode.localPath,
            )
        }
    }

    private fun EpisodeProgressEntity.mapToModel(): EpisodeProgress {
        return EpisodeProgress(
            episodeId = Identifier(
                localId = episodeLocalId,
                externalId = episodeExternalId,
            ),
            time = time,
            lastUpdatedAt = lastUpdatedAt
        )
    }

    private fun EpisodeDownloadEntity.mapToModel(): EpisodeDownload {
        return EpisodeDownload(
            episodeId = Identifier(
                localId = episodeLocalId,
                externalId = episodeExternalId,
            ),
            workId = workId,
            progress = progress,
            state = state,
            lastUpdatedAt = lastUpdatedAt
        )
    }

    private suspend fun List<EpisodeEmbeddedEntity>.probeDurations() =
        coroutineScope {
            map { entity ->
                async(Dispatchers.IO.limitedParallelism(5)) {
                    if (entity.episode.duration != UNSET_TIME) {
                        return@async entity
                    } else {
                        val episodeUrl = entity.episode.url

                        // probe with retry
                        repeat(2) {
                            val duration = metadataProbePlayer.probeDuration(episodeUrl)
                            if (duration != UNSET_TIME) {
                                return@async entity.copy(
                                    episode = entity.episode.copy(
                                        duration = duration
                                    )
                                )
                            }
                            delay(500)
                        }
                        return@async entity
                    }
                }
            }.awaitAll()
        }

    // Unreliable
    private suspend fun List<EpisodeEmbeddedEntity>.withDurations() = coroutineScope {
        map { entity ->
            withContext(Dispatchers.IO.limitedParallelism(3)) {
                async {
                    if (entity.episode.duration == UNSET_TIME) {
                        val duration = withTimeoutOrNull(5_000) {
                            entity.getMetadataDurationBlocking()
                        }  ?: UNSET_TIME

                        entity.copy(
                            episode = entity.episode.copy(duration = duration)
                        )
                    } else {
                        entity
                    }
                }
            }
        }.awaitAll()
    }

//    private suspend fun List<BookEpisode>.withIsDownloaded() = coroutineScope {
//        map { bookEpisode ->
//            withContext(Dispatchers.IO) {
//                async {
//                    val downloadManager = downloadManagerHelper.getDownloadManager()
//                    val download = downloadManager.downloadIndex.getDownload(bookEpisode.url)
//                    bookEpisode.copy(
//                        isDownloaded = download != null && download.state == Download.STATE_COMPLETED
//                    )
//                }
//            }
//        }.awaitAll()
//    }

    private fun EpisodeEmbeddedEntity.getMetadataDurationBlocking(): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(episode.url, HashMap())
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    private fun List<BookEmbeddedEntity>.bookEntities() = map { it.book }

    private fun List<BookEmbeddedEntity>.episodeEntities() = flatMap { it.episodes.map { e -> e.episode } }

    private fun List<BookEmbeddedEntity>.episodeProgressEntities() = flatMap { it.episodes.map { e -> e.episodeProgress } }

    private fun BookEmbeddedEntity.episodeEntities() = episodes.map { it.episode }

    private fun BookEmbeddedEntity.episodeProgressEntities() = episodes.map { it.episodeProgress }

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