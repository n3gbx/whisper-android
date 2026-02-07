package org.n3gbx.whisper.database.converter

import androidx.room.TypeConverter
import java.util.UUID

class UuidConverter {

    @TypeConverter
    fun stringToUuid(input: String?): UUID? {
        return input?.let { UUID.fromString(input) }
    }

    @TypeConverter
    fun uuidToString(input: UUID?): String? {
        return input?.toString()
    }
}