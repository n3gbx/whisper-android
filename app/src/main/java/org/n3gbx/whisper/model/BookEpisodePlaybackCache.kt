package org.n3gbx.whisper.model

import java.time.LocalDateTime
import kotlin.math.roundToInt

data class BookEpisodePlaybackCache(
    val duration: Long,
    val lastTime: Long,
    val lastUpdatedAt: LocalDateTime
) {
    val isFinished: Boolean
        get() = progressPercentage == 100

    val progressPercentage: Int
        get() {
            return if (duration > 0) {
                ((lastTime.toFloat() / duration.toFloat()) * 100).roundToInt()
            } else {
                0
            }
        }
}
