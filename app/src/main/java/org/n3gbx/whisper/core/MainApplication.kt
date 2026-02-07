package org.n3gbx.whisper.core

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import org.n3gbx.whisper.core.worker.custom.MainDelegationWorkerFactory

@HiltAndroidApp
class MainApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerFactoryEntryPoint {
        fun workerFactory(): MainDelegationWorkerFactory
    }

    override fun onCreate() {
        val workManagerConfiguration: Configuration = Configuration.Builder()
            .setWorkerFactory(EntryPoints.get(this, WorkerFactoryEntryPoint::class.java).workerFactory())
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()
        WorkManager.initialize(this, workManagerConfiguration)
        super.onCreate()
    }
}