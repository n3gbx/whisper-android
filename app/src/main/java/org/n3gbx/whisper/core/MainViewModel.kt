package org.n3gbx.whisper.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.n3gbx.whisper.core.worker.EpisodeDurationProbeWorker
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.data.SettingsRepository
import org.n3gbx.whisper.model.ApplicationTheme
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val applicationThemeState: StateFlow<ApplicationTheme> =
        settingsRepository.getThemeSetting()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ApplicationTheme.SYSTEM
            )

    fun probeEpisodeDurationsPeriodically() {
        PeriodicWorkRequestBuilder<EpisodeDurationProbeWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
    }

    fun reconcileDownloadedEpisodeFiles() {
        viewModelScope.launch {
            val episodes = episodeRepository.getDownloadedEpisodes().first()

            episodes.forEach { episode ->
                val localPath = episode.episode.localPath

                if (localPath != null) {
                    val file = File(localPath)
                    if (!file.exists()) {
                        episodeRepository.clearEpisodeLocalPath(episode.episode.localId)
                    }
                }
            }
        }
    }
}