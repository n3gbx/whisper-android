package org.n3gbx.whisper.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class EpisodeEmbeddedEntity(
    @Embedded
    val episode: EpisodeEntity,

    @Relation(
        parentColumn = "id_localId",
        entityColumn = "episodeId_localId"
    )
    val episodeProgress: EpisodeProgressEntity,

    @Relation(
        parentColumn = "id_localId",
        entityColumn = "episodeId_localId"
    )
    val episodeDownload: EpisodeDownloadEntity?,
)
