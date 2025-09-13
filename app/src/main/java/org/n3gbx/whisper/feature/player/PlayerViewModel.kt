package org.n3gbx.whisper.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.MainPlaybackService
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
): ViewModel() {
    private lateinit var controller: MediaController

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private val rewindTimeMs = 10000L
    private val rewindActions = MutableSharedFlow<RewindAction>()

    init {
        initController()
    }

    private fun initController() {
        val sessionToken = SessionToken(context, ComponentName(context, MainPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                try {
                    controller = controllerFuture.get()
                    controller.addListener(ControllerListener())

                    observePlayerPlaybackTime()
                    observeRewindActions()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 
            ContextCompat.getMainExecutor(context)
        )
    }

    fun onNewMedia(mediaItem: MediaItem) {
        controller.stop()
        controller.clearMediaItems()
        controller.addMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    fun onPlayPauseButtonClick() {
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun onStopButtonClick() {
        controller.stop()
        controller.clearMediaItems()
    }

    fun onRewindBackwardButtonClick() {
        viewModelScope.launch {
            rewindActions.emit(RewindAction.BACKWARD)
        }
    }

    fun onRewindForwardButtonClick() {
        viewModelScope.launch {
            rewindActions.emit(RewindAction.FORWARD)
        }
    }

    fun onSliderValueChange(value: Float) {
        _uiState.update {
            it.copy(sliderValue = value)
        }
    }

    fun onSliderValueChangeFinished() {
        _uiState.update {
            it.copy(currentTime = it.sliderValue.toLong())
        }
        controller.seekTo(_uiState.value.sliderValue.toLong())
    }

    private fun observePlayerPlaybackTime() {
        viewModelScope.launch {
            while (true) {
                val currentDuration = controller.duration
                if (currentDuration != C.TIME_UNSET) {
                    _uiState.update {
                        it.copy(durationTime = currentDuration)
                    }
                }

                if (controller.isPlaying) {
                    val currentPosition = controller.currentPosition
                    _uiState.update {
                        it.copy(
                            currentTime = currentPosition,
                            sliderValue = currentPosition.toFloat()
                        )
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun observeRewindActions() {
        viewModelScope.launch {
            var accumulatedDelta = 0L
            rewindActions
                .onEach { _ -> accumulatedDelta += rewindTimeMs }
                .debounce(300)
                .collect {
                    val newPosition = when (it) {
                        RewindAction.BACKWARD -> controller.currentPosition - accumulatedDelta
                        RewindAction.FORWARD -> controller.currentPosition + accumulatedDelta
                    }
                    controller.seekTo(newPosition.coerceIn(0, controller.duration))
                    accumulatedDelta = 0L
                }
        }
    }

    inner class ControllerListener: Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _uiState.update {
                it.copy(currentMedia = mediaItem)
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            _uiState.update {
                it.copy(isLoading = isLoading)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update {
                it.copy(isPlaying = isPlaying)
            }
        }
    }

    private enum class RewindAction {
        BACKWARD, FORWARD;
    }
}