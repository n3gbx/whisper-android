package org.n3gbx.whisper.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_IS_PLAYING_CHANGED
import androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.n3gbx.whisper.MainPlaybackService
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.data.BookmarkRepository
import org.n3gbx.whisper.feature.player.PlayerViewModel.RewindAction.BACKWARD
import org.n3gbx.whisper.feature.player.PlayerViewModel.RewindAction.FORWARD
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.BookEpisode
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val bookmarkRepository: BookmarkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private var currentBookId: String? = null
    private var bookJob: Job? = null
    private val rewindTimeMs = 10000L
    private val rewindActions = MutableSharedFlow<RewindAction>()
    private var isSliderValueChangeInProgress: Boolean = false

    private lateinit var controller: MediaController

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<PlayerUiEvent>()
    val uiEvents: SharedFlow<PlayerUiEvent> = _uiEvents.asSharedFlow()

    init {
        initController()
    }

    fun setBook(bookId: String?) {
        if (::controller.isInitialized && bookId != null && currentBookId != bookId) {
            observeBook(bookId)
        }
    }

    fun onPlayPauseButtonClick() {
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun onDismissButtonClick() {
        // stop player
        controller.pause()
        controller.stop()
        controller.clearMediaItems()

        // stop book observation
        currentBookId = null
        bookJob?.cancel()

        // reset ui state
        _uiState.update { PlayerUiState() }
    }

    fun onRewindBackwardButtonClick() {
        viewModelScope.launch {
            rewindActions.emit(BACKWARD)
        }
    }

    fun onRewindForwardButtonClick() {
        viewModelScope.launch {
            rewindActions.emit(FORWARD)
        }
    }

    fun onSliderValueChange(value: Float) {
        isSliderValueChangeInProgress = true
        _uiState.updateSliderValue(value)
    }

    fun onSliderValueChangeFinished(value: Float) {
        isSliderValueChangeInProgress = false
        controller.seekTo(value.toLong())
    }

    fun onEpisodeClick(index: Int) {
        if (_uiState.value.book?.recentEpisodeIndex != index) {
            if (_uiState.getEpisodeByIndex(index)?.playbackCache?.isFinished == true) {
                controller.seekTo(index, 0)
            } else {
                val time = _uiState.getEpisodeCachedPlaybackPositionByIndex(index)
                controller.seekTo(index, time)
            }
        }
    }

    fun onBookmarkButtonClick() {
        currentBookId?.let { bookId ->
            viewModelScope.launch {
                bookmarkRepository.changeBookmark(bookId)
            }
        }
    }

    private suspend fun cacheEpisodePlaybackPosition(
        episodeId: String?,
        duration: Long,
        currentTime: Long
    ) {
        currentBookId?.let { bookId ->
            if (duration != C.TIME_UNSET && episodeId != null) {
                bookRepository.saveBookEpisodePlayback(
                    bookId = bookId,
                    episodeId = episodeId,
                    duration = duration,
                    currentTime = currentTime
                )
            }
        }
    }

    private fun observePlaybackPositionForPositionCache() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)

                val episodeId = controller.currentMediaItem?.mediaId
                val duration = controller.duration
                val currentTime = controller.currentPosition

                episodeId?.let {
                    cacheEpisodePlaybackPosition(episodeId, duration, currentTime)
                }
            }
        }
    }

    private fun observePlaybackPositionForStateUpdate() {
        viewModelScope.launch {
            while (isActive) {
                yield()
                controller.duration.also { duration ->
                    if (duration != C.TIME_UNSET) {
                        if (_uiState.value.duration != duration) {
                            _uiState.updateDuration(duration)
                        }

                        controller.currentPosition.also { currentPosition ->
                            if (_uiState.value.currentTime != currentPosition && !isSliderValueChangeInProgress) {
                                _uiState.updateSliderValueAndCurrentTime(currentPosition.toFloat())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeRewindActionsForPlaybackPositionUpdate() {
        viewModelScope.launch {
            var accumulatedDelta = 0L
            rewindActions
                .onEach { _ -> accumulatedDelta += rewindTimeMs }
                .debounce(300)
                .collect { rewindAction ->
                    val newPosition = when (rewindAction) {
                        BACKWARD -> controller.currentPosition - accumulatedDelta
                        FORWARD -> controller.currentPosition + accumulatedDelta
                    }.coerceIn(0, controller.duration)

                    controller.seekTo(newPosition)

                    accumulatedDelta = 0L
                }
        }
    }

    private fun observeBook(bookId: String) {
        bookJob?.cancel()

        bookJob = viewModelScope.launch {
            bookRepository.getBook(bookId)
                .onStart {
                    _uiState.update {
                        it.copy(isLoading = true)
                    }
                }.collect { book ->
                    if (book == null) {
                        navigateBackWithMessage("Not a valid book identifier")
                    } else {
                        if (controller.currentMediaItem == null || currentBookId != bookId) {
                            val mediaItems = book.episodesAsMediaItems()
                            val currentIndex = book.recentEpisodeIndex
                            val currentTime = book.recentEpisode.playbackCache?.lastTime ?: 0

                            controller.stop()
                            controller.clearMediaItems()
                            controller.setMediaItems(mediaItems, currentIndex, currentTime)
                            controller.prepare()

                            _uiState.updateSliderValueAndCurrentTime(currentTime.toFloat())
                        }

                        _uiState.update {
                            it.copy(
                                book = book,
                                isLoading = false
                            )
                        }

                        currentBookId = bookId
                    }
                }
        }
    }

    private fun navigateBackWithMessage(message: String) {
        viewModelScope.launch {
            _uiEvents.emit(PlayerUiEvent.NavigateBack)
            _uiEvents.emit(PlayerUiEvent.ShowMessage(message))
        }
    }

    private fun Book.episodesAsMediaItems(): List<MediaItem> {
        return episodes.map { episode ->
            MediaItem.Builder()
                .setMediaId(episode.id)
                .setUri(episode.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setComposer(narrator)
                        .setDescription(description)
                        .setArtist(author)
                        .setTitle(title)
                        .setArtworkUri(coverUrl?.toUri())
                        .build()
                )
                .build()
        }
    }

    private fun initController() {
        val sessionToken = SessionToken(context, ComponentName(context, MainPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                try {
                    controller = controllerFuture.get()
                    controller.addListener(ControllerListener())

                    observePlaybackPositionForPositionCache()
                    observePlaybackPositionForStateUpdate()
                    observeRewindActionsForPlaybackPositionUpdate()
                } catch (e: Exception) {
                    navigateBackWithMessage("Error while initialising player")
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun MutableStateFlow<PlayerUiState>.updateDuration(value: Long) {
        update {
            it.copy(duration = value)
        }
    }

    private fun MutableStateFlow<PlayerUiState>.updateSliderValue(value: Float) {
        update {
            it.copy(sliderValue = value)
        }
    }

    private fun MutableStateFlow<PlayerUiState>.updateSliderValueAndCurrentTime(value: Float) {
        update {
            it.copy(
                sliderValue = value,
                currentTime = value.toLong()
            )
        }
    }

    private fun MutableStateFlow<PlayerUiState>.getCurrentEpisodeCachedPlaybackPosition(): Long {
        return value.book?.recentEpisode?.playbackCache?.lastTime ?: 0
    }

    private fun MutableStateFlow<PlayerUiState>.getEpisodeCachedPlaybackPositionByIndex(index: Int): Long {
        return value.book?.episodes?.getOrNull(index)?.playbackCache?.lastTime ?: 0
    }

    private fun MutableStateFlow<PlayerUiState>.getEpisodeByIndex(index: Int): BookEpisode? {
        return value.book?.episodes?.getOrNull(index)
    }

    private fun MutableStateFlow<PlayerUiState>.getEpisodeById(id: String?): BookEpisode? {
        return value.book?.episodes?.find { it.id == id }
    }

    inner class ControllerListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val expectedEvents = listOf(
                EVENT_IS_PLAYING_CHANGED, // playing/paused
                EVENT_POSITION_DISCONTINUITY, // seek/jump
                EVENT_MEDIA_ITEM_TRANSITION // new
            )

            if (events.containsAny(*expectedEvents.toIntArray())) {
                viewModelScope.launch {
                    cacheEpisodePlaybackPosition(
                        episodeId = player.currentMediaItem?.mediaId,
                        duration = player.duration,
                        currentTime = player.currentPosition
                    )
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                val prevIndex = controller.previousMediaItemIndex
                val prevEpisode = _uiState.getEpisodeByIndex(prevIndex)

                if (prevEpisode != null) {
                    viewModelScope.launch {
                        cacheEpisodePlaybackPosition(
                            episodeId = prevEpisode.id,
                            duration = prevEpisode.duration,
                            currentTime = prevEpisode.duration
                        )
                    }
                }
            }

            _uiState.getEpisodeById(mediaItem?.mediaId)?.let { newEpisode ->
                _uiState.update {
                    it.copy(
                        book = it.book?.copy(
                            recentEpisode = newEpisode
                        )
                    )
                }
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            _uiState.update {
                it.copy(isBuffering = isLoading)
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