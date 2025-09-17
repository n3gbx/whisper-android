package org.n3gbx.whisper.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "book_episode",
    indices = [
        Index(value = ["externalBookId", "title"], unique = true)
    ]
)
data class BookEpisodeEntity(
    @PrimaryKey val id: String = Uuid.random().toString(),
    val bookId: String,
    val externalBookId: String,
    val externalId: String,
    val title: String,
    val url: String,
    val duration: Long
)
