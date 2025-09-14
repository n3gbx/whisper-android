package org.n3gbx.whisper.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.n3gbx.whisper.database.converter.LocalDateConverter
import org.n3gbx.whisper.database.converter.LocalDateTimeConverter
import org.n3gbx.whisper.database.dao.BookmarkDao
import org.n3gbx.whisper.database.dao.BookEpisodePlaybackCacheDao
import org.n3gbx.whisper.database.entity.BookmarkEntity
import org.n3gbx.whisper.database.entity.BookEpisodePlaybackCacheEntity

@Database(
    entities = [
        BookmarkEntity::class,
        BookEpisodePlaybackCacheEntity::class
    ],
    version = 1
)
@TypeConverters(
    LocalDateTimeConverter::class,
    LocalDateConverter::class
)
abstract class MainDatabase : RoomDatabase() {

    abstract fun bookmarkDao() : BookmarkDao
    abstract fun bookEpisodePlaybackCache() : BookEpisodePlaybackCacheDao
}