package org.n3gbx.whisper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episode",
    indices = [
        Index(value = ["bookId_externalId", "title"], unique = true)
    ]
)
data class EpisodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_localId")
    val localId: String,

    @ColumnInfo(name = "id_externalId")
    val externalId: String,

    @ColumnInfo(name = "bookId_localId")
    val bookLocalId: String,

    @ColumnInfo(name = "bookId_externalId")
    val bookExternalId: String,

    val title: String,
    val url: String,
    val duration: Long,
    val localPath: String?,
)
