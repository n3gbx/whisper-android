package org.n3gbx.whisper.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.n3gbx.whisper.database.converter.LocalDateConverter
import org.n3gbx.whisper.database.converter.LocalDateTimeConverter
import org.n3gbx.whisper.database.dao.BookDao
import org.n3gbx.whisper.database.entity.BookEntity
import org.n3gbx.whisper.database.entity.BookEpisodeEntity
import org.n3gbx.whisper.database.entity.BookEpisodeProgressEntity

@Database(
    entities = [
        BookEpisodeProgressEntity::class,
        BookEntity::class,
        BookEpisodeEntity::class,
    ],
    version = 1
)
@TypeConverters(
    LocalDateTimeConverter::class,
    LocalDateConverter::class
)
abstract class MainDatabase : RoomDatabase() {

    abstract fun bookDao() : BookDao
}