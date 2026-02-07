package org.n3gbx.whisper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.n3gbx.whisper.model.DownloadState
import java.time.LocalDateTime

@Entity(
    tableName = "episode_download",
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
data class EpisodeDownloadEntity(
    @PrimaryKey
    @ColumnInfo(name = "episodeId_localId")
    val episodeLocalId: String,

    @ColumnInfo(name = "episodeId_externalId")
    val episodeExternalId: String,

    val workId: String?,
    val progress: Int,
    val state: DownloadState,
    val lastUpdatedAt: LocalDateTime = LocalDateTime.now(),
)
