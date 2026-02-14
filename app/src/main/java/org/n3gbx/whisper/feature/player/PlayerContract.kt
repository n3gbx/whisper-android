package org.n3gbx.whisper.feature.player

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.R
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.StringResource
import org.n3gbx.whisper.model.StringResource.Companion.fromRes

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
    val label: StringResource
) {
    X0_25(0.25f, fromRes(R.string.player_speed_0_25x)),
    X0_75(0.75f, fromRes(R.string.player_speed_0_75x)),
    X1(1f, fromRes(R.string.player_speed_1x)),
    X1_25(1.25f, fromRes(R.string.player_speed_1_25x)),
    X1_50(1.5f, fromRes(R.string.player_speed_1_50x))
}

@Immutable
enum class SleepTimerOption(
    val value: Long,
    val label: StringResource
) {
    FIVE_MINUTES(5 * 60 * 1000, fromRes(R.string.player_timer_5_minutes)),
    FIFTEEN_MINUTES(15 * 60 * 1000, fromRes(R.string.player_timer_15_minutes)),
    THIRTY_MINUTES(30 * 60 * 1000, fromRes(R.string.player_timer_30_minutes)),
    FORTY_FIVE_MINUTES(45 * 60 * 1000, fromRes(R.string.player_timer_45_minutes)),
    SIXTY_MINUTES(60 * 60 * 1000, fromRes(R.string.player_timer_60_minutes)),
    END_OF_EPISODE(-1L, fromRes(R.string.player_timer_end_of_episode))
}
