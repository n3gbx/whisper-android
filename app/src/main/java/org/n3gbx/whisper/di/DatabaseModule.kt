package org.n3gbx.whisper.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.n3gbx.whisper.database.MainDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideRoomDatabase(@ApplicationContext context: Context): MainDatabase =
        Room.databaseBuilder(
            context = context,
            klass = MainDatabase::class.java,
            name = "whisper-database"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Singleton
    @Provides
    fun provideFirestoreDatabase() = Firebase.firestore

    @Singleton
    @Provides
    fun provideBookDao(mainDatabase: MainDatabase) = mainDatabase.bookDao()

    @Singleton
    @Provides
    fun provideEpisodeDao(mainDatabase: MainDatabase) = mainDatabase.episodeDao()

    @Singleton
    @Provides
    fun provideEpisodeProgressDao(mainDatabase: MainDatabase) = mainDatabase.episodeProgressDao()

    @Singleton
    @Provides
    fun provideEpisodeDownloadDao(mainDatabase: MainDatabase) = mainDatabase.episodeDownloadDao()
}