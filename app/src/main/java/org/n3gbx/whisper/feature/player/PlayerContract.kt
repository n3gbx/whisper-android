package org.n3gbx.whisper.feature.player

import org.n3gbx.whisper.model.Book

data class PlayerUiState(
    val book: Book? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val durationTime: Long = 0,
    val currentTime: Long = 0,
    val sliderValue: Float = 0f,
) {
    val remainingTime: Long?
        get() = if (durationTime > 0) durationTime - currentTime else null

    val sliderValueRange: ClosedFloatingPointRange<Float>
        get() = 0f..durationTime.toFloat()

    val progressValue: Float
        get() = if (durationTime > 0 ) currentTime.toFloat() / durationTime.toFloat() else 0f
}

sealed interface PlayerUiEvent {
    data class ShowMessage(val message: String): PlayerUiEvent
    data object NavigateBack: PlayerUiEvent
}
