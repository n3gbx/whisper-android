package org.n3gbx.whisper.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookEmbeddedEntity(
    @Embedded
    val book: BookEntity,

    @Relation(
        entity = EpisodeEntity::class,
        parentColumn = "id_localId",
        entityColumn = "bookId_localId",
    )
    val episodes: List<EpisodeEmbeddedEntity>
)
