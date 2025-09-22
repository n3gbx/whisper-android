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
    val showDescription: Boolean = false,
    val isDescriptionButtonVisible: Boolean = false,
    val isBookmarkButtonVisible: Boolean = false,
    val selectedSpeedOption: SpeedOption = SpeedOption.X1,
    val selectedSleepTimerOption: SleepTimerOption? = null,
) {
    val remainingTime: Long?
        get() = if (duration > 0) duration - currentTime else null

    val sliderValueRange: ClosedFloatingPointRange<Float>
        get() = 0f..duration.toFloat()

    val progressValue: Float
        get() = if (duration > 0 ) currentTime.toFloat() / duration.toFloat() else 0f

    val shouldDisableControls: Boolean
        get() = isBuffering || isLoading

    val speedOptions: List<SpeedOption> = SpeedOption.entries

    val sleepTimerOptions: List<SleepTimerOption> = SleepTimerOption.entries
}

sealed interface PlayerUiEvent {
    data class ShowMessage(val message: String): PlayerUiEvent
    data object NavigateBack: PlayerUiEvent
}

@Immutable
enum class SpeedOption(
    val value: Float,
    val label: String
) {
    X0_50(0.25f, "0.25x"),
    X0_75(0.75f, "0.75x"),
    X1(1f, "1x"),
    X1_25(1.25f, "1.25x"),
    X1_50(1.5f, "1.5x")
}

@Immutable
enum class SleepTimerOption(
    val value: Long,
    val label: String
) {
    FIVE_MINUTES(5 * 60 * 1000,"5 minutes"),
    FIFTEEN_MINUTES(15 * 60 * 1000, "15 minutes"),
    THIRTY_MINUTES(30 * 60 * 1000, "30 minutes"),
    FORTY_FIVE_MINUTES(45 * 60 * 1000, "45 minutes"),
    SIXTY_MINUTES(60 * 60 * 1000, "60 minutes"),
    END_OF_EPISODE(-1L, "End of episode")
}
