package org.n3gbx.whisper.database.converter

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

class LocalDateTimeConverter {

    @TypeConverter
    fun stringToLocalDateTime(input: String?): LocalDateTime? {
        return input?.let { LocalDateTime.parse(input) }
    }

    @TypeConverter
    fun localDateTimeToString(input: LocalDateTime?): String? {
        return input?.toString()
    }
}

class LocalDateConverter {

    @TypeConverter
    fun stringToLocalDate(input: String?): LocalDate? {
        return input?.let { LocalDate.parse(input) }
    }

    @TypeConverter
    fun localDateToString(input: LocalDate?): String? {
        return input?.toString()
    }
}