package org.n3gbx.whisper.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookEmbeddedEntity(
    @Embedded
    val book: BookEntity,

    @Relation(
        entity = BookEpisodeEntity::class,
        parentColumn = "id",
        entityColumn = "bookId"
    )
    val episodes: List<BookEpisodeEmbeddedEntity>
)
