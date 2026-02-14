package org.n3gbx.whisper.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.n3gbx.whisper.core.common.GetEpisodesCacheDir
import org.n3gbx.whisper.data.SettingsRepository
import javax.inject.Inject

class DeleteLocalDataUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val getEpisodesCacheDir: GetEpisodesCacheDir,
) {

    suspend operator fun invoke(): Boolean {
        val isDeleted = withContext(Dispatchers.IO) { getEpisodesCacheDir().deleteRecursively() }
        if (!isDeleted) return false

        settingsRepository.clearLocalData()
        return true
    }
}