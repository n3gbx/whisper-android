package org.n3gbx.whisper.data

import org.n3gbx.whisper.database.entity.BookEmbeddedEntity
import org.n3gbx.whisper.database.entity.EpisodeDownloadEntity
import org.n3gbx.whisper.database.entity.EpisodeEmbeddedEntity
import org.n3gbx.whisper.database.entity.EpisodeProgressEntity
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.Episode
import org.n3gbx.whisper.model.EpisodeDownload
import org.n3gbx.whisper.model.EpisodeProgress
import org.n3gbx.whisper.model.Identifier

object Mapper {

    fun List<BookEmbeddedEntity>.mapToModels(): List<Book> {
        return map { entity -> entity.mapToModel() }
    }

    fun BookEmbeddedEntity.mapToModel(): Book {
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

    fun List<EpisodeEmbeddedEntity>.mapToModel(): List<Episode> {
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

    fun EpisodeProgressEntity.mapToModel(): EpisodeProgress {
        return EpisodeProgress(
            episodeId = Identifier(
                localId = episodeLocalId,
                externalId = episodeExternalId,
            ),
            time = time,
            lastUpdatedAt = lastUpdatedAt
        )
    }

    fun EpisodeDownloadEntity.mapToModel(): EpisodeDownload {
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
}