package org.n3gbx.whisper.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "bookmark",
    indices = [
        Index(value = ["bookId"], unique = true)
    ]
)
data class BookmarkEntity(
    @PrimaryKey val id: String = Uuid.random().toString(),
    val bookId: String,
)
