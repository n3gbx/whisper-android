package org.n3gbx.whisper.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "book_episode_playback_cache",
    indices = [
        Index(value = ["bookId", "episodeId"], unique = true)
    ]
)
data class BookEpisodePlaybackCacheEntity(
    @PrimaryKey val id: String = Uuid.random().toString(),
    val bookId: String,
    val episodeId: String,
    val duration: Long,
    val lastTime: Long,
    val lastUpdatedAt: LocalDateTime
)
