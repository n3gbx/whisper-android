package org.n3gbx.whisper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "episode_progress",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id_localId"],
            childColumns = ["episodeId_localId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["episodeId_localId"])
    ]
)
data class EpisodeProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "episodeId_localId")
    val episodeLocalId: String,

    @ColumnInfo(name = "episodeId_externalId")
    val episodeExternalId: String,

    val time: Long,
    val lastUpdatedAt: LocalDateTime = LocalDateTime.now(),
)
