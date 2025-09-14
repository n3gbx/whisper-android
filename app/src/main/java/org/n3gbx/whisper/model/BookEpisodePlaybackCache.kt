package org.n3gbx.whisper.model

import java.time.LocalDateTime

data class BookEpisodePlaybackCache(
    val durationTime: Long,
    val lastTime: Long,
    val lastUpdatedAt: LocalDateTime
) {
    val isFinished: Boolean
        get() = progressPercentage == 100.0f

    val progressPercentage: Float
        get() {
            return if (durationTime > 0) {
                (lastTime.toFloat() / durationTime.toFloat()) * 100.toFloat()
            } else {
                0f
            }
        }
}
