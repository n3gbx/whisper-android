package org.n3gbx.whisper.database.converter

import androidx.room.TypeConverter
import java.time.LocalDate

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