package org.n3gbx.whisper.core

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.installations.installations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.n3gbx.whisper.core.worker.EpisodeDurationProbeWorker
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.data.SettingsRepository
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    init {
        viewModelScope.launch {
            settingsRepository.setInstallationId(FirebaseInstallations.getInstance().id.await())
        }
    }

    fun probeEpisodeDurationsPeriodically() {
        val request = PeriodicWorkRequestBuilder<EpisodeDurationProbeWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    fun reconcileDownloadedEpisodeFiles() {
        viewModelScope.launch {
            val episodes = episodeRepository.getDownloadedEpisodes().first()

            episodes.forEach { episode ->
                val localPath = episode.localPath

                if (localPath != null) {
                    val file = File(localPath)
                    if (!file.exists()) {
                        episodeRepository.clearEpisodeLocalPath(episode.id.localId)
                    }
                }
            }
        }
    }
}