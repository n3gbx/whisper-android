package org.n3gbx.whisper.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "book_episode_progress",
    indices = [
        Index(value = ["id", "episodeId", "externalEpisodeId"], unique = true)
    ]
)
data class BookEpisodeProgressEntity(
    @PrimaryKey val id: String = Uuid.random().toString(),
    val episodeId: String,
    val externalEpisodeId: String,
    val lastTime: Long,
    val lastUpdatedAt: LocalDateTime
)
