package org.n3gbx.whisper.model

import kotlin.math.roundToInt

data class BookEpisode(
    val id: Identifier,
    val bookId: Identifier,
    val title: String,
    val url: String,
    val duration: Long,
    val progress: BookEpisodeProgress
) {
    val isFinished: Boolean
        get() = progressPercentage == 100

    val progressValue: Float
        get() {
            return if (duration > 0) {
                progress.lastTime.toFloat() / duration.toFloat()
            } else {
                0f
            }
        }

    val progressPercentage: Int
        get() = (progressValue * 100).roundToInt()
}
