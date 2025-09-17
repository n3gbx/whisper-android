package org.n3gbx.whisper.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookEpisodeEmbeddedEntity(
    @Embedded
    val bookEpisode: BookEpisodeEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "episodeId"
    )
    val bookEpisodeProgress: BookEpisodeProgressEntity
)
