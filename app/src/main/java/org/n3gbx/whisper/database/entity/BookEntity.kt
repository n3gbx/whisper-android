package org.n3gbx.whisper.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "book",
    indices = [
        Index(value = ["externalId"], unique = true)
    ]
)
data class BookEntity (
    @PrimaryKey val id: String = Uuid.random().toString(),
    val externalId: String,
    val title: String,
    val author: String,
    val narrator: String?,
    val coverUrl: String?,
    val description: String?,
    val isBookmarked: Boolean,
) {

    class Update(
        val id: String,
        val isBookmarked: Boolean
    )
}
