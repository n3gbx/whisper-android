package org.n3gbx.whisper.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entity")
data class Entity(
    @PrimaryKey val id: Int,
)
