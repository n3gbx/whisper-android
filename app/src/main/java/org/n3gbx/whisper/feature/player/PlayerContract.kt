package org.n3gbx.whisper.feature.player

import androidx.media3.common.MediaItem

data class PlayerUiState(
    val currentMedia: MediaItem? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val durationTime: Long = 0,
    val currentTime: Long = 0,
    val sliderValue: Float = 0f,
) {
    private val remainingTime: Long
        get() = durationTime - currentTime

    val sliderValueRange: ClosedFloatingPointRange<Float>
        get() = 0f..durationTime.toFloat()

    val progressValue: Float
        get() = if (durationTime > 0 ) currentTime.toFloat() / durationTime.toFloat() else 0f

    val formattedCurrentTime: String
        get() = currentTime.convertToText()

    val formattedRemainingTime: String
        get() = if (remainingTime >= 0) remainingTime.convertToText() else "--:--"

    private fun Long.convertToText(): String {
        val sec = this / 1000
        val minutes = sec / 60
        val seconds = sec % 60

        val minutesString = if (minutes < 10) {
            "0$minutes"
        } else {
            minutes.toString()
        }
        val secondsString = if (seconds < 10) {
            "0$seconds"
        } else {
            seconds.toString()
        }
        return "$minutesString:$secondsString"
    }
}
