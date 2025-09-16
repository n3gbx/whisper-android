package org.n3gbx.whisper.feature.player

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.model.Book

@Immutable
data class PlayerUiState(
    val book: Book? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val isLoading: Boolean = true,
    val duration: Long = 0,
    val currentTime: Long = 0,
    val sliderValue: Float = 0f,
) {
    val remainingTime: Long?
        get() = if (duration > 0) duration - currentTime else null

    val sliderValueRange: ClosedFloatingPointRange<Float>
        get() = 0f..duration.toFloat()

    val progressValue: Float
        get() = if (duration > 0 ) currentTime.toFloat() / duration.toFloat() else 0f

    val shouldDisableControls: Boolean
        get() = isBuffering || isLoading
}

sealed interface PlayerUiEvent {
    data class ShowMessage(val message: String): PlayerUiEvent
    data object NavigateBack: PlayerUiEvent
}
