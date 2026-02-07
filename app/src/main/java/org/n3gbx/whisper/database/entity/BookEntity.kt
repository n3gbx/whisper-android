package org.n3gbx.whisper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book",
    indices = [
        Index(value = ["id_externalId"], unique = true)
    ]
)
data class BookEntity (
    @PrimaryKey
    @ColumnInfo(name = "id_localId")
    val localId: String,

    @ColumnInfo(name = "id_externalId")
    val externalId: String,

    val title: String,
    val author: String,
    val narrator: String?,
    val coverUrl: String?,
    val description: String?,
    val isBookmarked: Boolean,
) {

    class Update(
        @ColumnInfo(name = "id_localId")
        val localId: String,

        val isBookmarked: Boolean
    )
}
