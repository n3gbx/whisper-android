package org.n3gbx.whisper.model

import org.n3gbx.whisper.Constants.UNSET_TIME
import kotlin.math.roundToInt

data class Episode(
    val id: Identifier,
    val bookId: Identifier,
    val title: String,
    val url: String,
    val duration: Long,
    val progress: EpisodeProgress,
    val download: EpisodeDownload?,
    val localPath: String?,
) {
    val hasError: Boolean
        get() = duration == UNSET_TIME

    val isFinished: Boolean
        get() = progressPercentage == 100

    val progressValue: Float
        get() {
            return if (duration > 0) {
                progress.time.toFloat() / duration.toFloat()
            } else {
                0f
            }
        }

    val progressPercentage: Int
        get() = (progressValue * 100).roundToInt()

    val progressType: ProgressType
        get() = when(progressValue) {
            1f -> ProgressType.FINISHED
            0f -> ProgressType.NOT_STARTED
            else -> ProgressType.STARTED
        }
}
