package org.n3gbx.whisper.domain

import org.n3gbx.whisper.core.common.GetEpisodesCacheDir
import java.io.File
import javax.inject.Inject

class GetEpisodesCacheDirSizeUseCase @Inject constructor(
    private val getEpisodesCacheDir: GetEpisodesCacheDir,
) {

    operator fun invoke() = getDirSize(getEpisodesCacheDir())

    private fun getDirSize(dir: File): Long {
        var totalLength = 0L
        (dir.listFiles() ?: emptyArray()).forEach { file ->
            totalLength += if (file.isFile) file.length() else getDirSize(file)
        }
        return totalLength
    }
}