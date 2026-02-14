package org.n3gbx.whisper.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.n3gbx.whisper.database.converter.LocalDateConverter
import org.n3gbx.whisper.database.converter.LocalDateTimeConverter
import org.n3gbx.whisper.database.converter.UuidConverter
import org.n3gbx.whisper.database.dao.BookDao
import org.n3gbx.whisper.database.dao.EpisodeDao
import org.n3gbx.whisper.database.dao.EpisodeDownloadDao
import org.n3gbx.whisper.database.dao.EpisodeProgressDao
import org.n3gbx.whisper.database.entity.BookEntity
import org.n3gbx.whisper.database.entity.EpisodeDownloadEntity
import org.n3gbx.whisper.database.entity.EpisodeEntity
import org.n3gbx.whisper.database.entity.EpisodeProgressEntity

@Database(
    entities = [
        EpisodeProgressEntity::class,
        EpisodeDownloadEntity::class,
        BookEntity::class,
        EpisodeEntity::class,
    ],
    version = 3
)
@TypeConverters(
    LocalDateTimeConverter::class,
    LocalDateConverter::class,
    UuidConverter::class,
)
abstract class MainDatabase : RoomDatabase() {

    abstract fun bookDao() : BookDao
    abstract fun episodeDao() : EpisodeDao
    abstract fun episodeProgressDao() : EpisodeProgressDao
    abstract fun episodeDownloadDao() : EpisodeDownloadDao

    fun clear() {
        clearAllTables()
    }
}