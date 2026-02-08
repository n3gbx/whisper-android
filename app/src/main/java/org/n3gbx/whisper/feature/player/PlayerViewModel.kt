package org.n3gbx.whisper.feature.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.C.INDEX_UNSET
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_IS_PLAYING_CHANGED
import androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.feature.player.PlayerViewModel.RewindAction.BACKWARD
import org.n3gbx.whisper.feature.player.PlayerViewModel.RewindAction.FORWARD
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.DownloadState
import org.n3gbx.whisper.model.Episode
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import org.n3gbx.whisper.core.service.PlayerPlaybackService
import org.n3gbx.whisper.core.service.PlayerPlaybackService.CustomCommand.CANCEL_SLEEP_TIMER
import org.n3gbx.whisper.core.service.PlayerPlaybackService.CustomCommand.START_SLEEP_TIMER
import org.n3gbx.whisper.core.common.EpisodeDownloadManager
import org.n3gbx.whisper.core.common.IsConnectedToInternet
import org.n3gbx.whisper.data.SettingsRepository
import org.n3gbx.whisper.model.ConnectionType.NONE
import org.n3gbx.whisper.model.ConnectionType.WIFI
import org.n3gbx.whisper.utils.connectionTypeFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
    private val episodeRepository: EpisodeRepository,
    private val episodeDownloadManager: EpisodeDownloadManager,
    private val isConnectedToInternetFlow: IsConnectedToInternet,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private var currentBookId: Identifier? = null
    private var bookJob: Job? = null
    private val rewindTimeMs = 10000L
    private val rewindActions = MutableSharedFlow<RewindAction>()
    private var pendingSliderValue: Float? = null
    private var isSliderValueChangePending: Boolean = false

    private lateinit var controller: MediaController

    private val isAutoDownloadEnabledState = combine(
        context.connectionTypeFlow(),
        settingsRepository.getAutoDownloadSetting(),
        settingsRepository.getDownloadWifiOnlySetting(),
    ) { connectionType, isEnabled, isWifiOnly ->
        if (isWifiOnly) connectionType == WIFI && isEnabled
        else connectionType != NONE && isEnabled
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private val isDownloadEnabledState = combine(
        context.connectionTypeFlow(),
        settingsRepository.getDownloadWifiOnlySetting(),
    ) { connectionType, isWifiOnly ->
        if (isWifiOnly) connectionType == WIFI
        else connectionType != NONE
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<PlayerUiEvent>()
    val uiEvents: SharedFlow<PlayerUiEvent> = _uiEvents.asSharedFlow()

    private val autoPlayTrigger =
        _uiState
            .map { !it.isLoading && !it.isBuffering }
            .distinctUntilChanged()
            .filter { it }
            .map { Unit }

    init {
        initController()
        observeAutoPlayTrigger()
        observeAutoDownload()
        observePlaybackSource()
    }

    fun setBook(bookId: Identifier?) {
        if (::controller.isInitialized && bookId != null && currentBookId != bookId) {
            observeBook(bookId)
        }
    }

    fun onSpeedOptionChange(speedOption: SpeedOption) {
        _uiState.update {
            it.copy(selectedSpeedOption = speedOption)
        }
        controller.setPlaybackSpeed(speedOption.value)
        sendMessageEvent("Speed set: ${speedOption.label}")
    }

    fun onSleepTimerOptionChange(sleepTimerOption: SleepTimerOption?) {
        _uiState.update {
            it.copy(selectedSleepTimerOption = sleepTimerOption)
        }

        if (sleepTimerOption != null) {
            val commandExtras = Bundle().apply { putLong("duration", sleepTimerOption.value) }

            controller.sendCustomCommand(
                SessionCommand(START_SLEEP_TIMER.name, commandExtras),
                Bundle.EMPTY
            )
            sendMessageEvent("Sleep timer set: ${sleepTimerOption.label}")
        } else {
            controller.sendCustomCommand(
                SessionCommand(CANCEL_SLEEP_TIMER.name, Bundle.EMPTY),
                Bundle.EMPTY
            )
            sendMessageEvent("Sleep timer cancelled")
        }
    }

    fun onDescriptionButtonClick() {
        _uiState.update {
            it.copy(showDescription = true)
        }
    }

    fun onDescriptionDismiss() {
        _uiState.update {
            it.copy(showDescription = false)
        }
    }

    fun onPlayPauseButtonClick() {
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun onMiniPlayerDismissButtonClicked() {
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
        isSliderValueChangePending = true
        pendingSliderValue = value

        _uiState.updateSliderValue(value)
    }

    fun onSliderValueChangeFinished() {
        val value = pendingSliderValue ?: return
        pendingSliderValue = null
        isSliderValueChangePending = false

        controller.seekTo(value.toLong())
    }

    fun onEpisodeClick(index: Int) {
        val isLoading = _uiState.value.isLoading
        val isBuffering = _uiState.value.isBuffering
        val isSameEpisode = _uiState.value.book?.recentEpisodeIndex == index

        if (isLoading || isBuffering || isSameEpisode) return

        if (_uiState.getEpisodeByIndex(index)?.isFinished == true) {
            controller.seekTo(index, 0) // start over if finished
        } else {
            val time = _uiState.getEpisodeCachedPlaybackPositionByIndex(index)
            controller.seekTo(index, time) // resume on where left off
        }
    }

    fun onEpisodeDownloadClick(index: Int) {
        if (isDownloadEnabledState.value) {
            _uiState.getEpisodeByIndex(index)?.let {
                downloadEpisode(it)
            }
        } else {
            sendMessageEvent("Download is disabled by settings constraints")
        }
    }

    fun onBookmarkButtonClick() {
        currentBookId?.let { bookId ->
            viewModelScope.launch {
                bookRepository.updateBookBookmark(bookId)
            }
        }
    }

    private suspend fun saveEpisodePlaybackProgress(
        externalEpisodeId: String?,
        currentTime: Long
    ) {
        currentBookId?.let { bookId ->
            if (currentTime != C.TIME_UNSET && externalEpisodeId != null) {
                episodeRepository.updateEpisodeProgress(
                    bookExternalId = bookId.externalId,
                    episodeExternalId = externalEpisodeId,
                    currentTime = currentTime
                )
            }
        }
    }

    private fun downloadEpisode(episode: Episode) {
        viewModelScope.launch {
            val isCancellable =
                episode.download?.state == DownloadState.QUEUED ||
                        episode.download?.state == DownloadState.PROGRESSING

            val episodeLocalId = episode.id.localId

            when {
                episode.download == null -> {
                    val workId = episodeDownloadManager.enqueueDownload(
                        bookLocalId = episode.bookId.localId,
                        episodeLocalId = episode.id.localId,
                        episodeUrl = episode.url,
                    )
                    episodeRepository.markEpisodeDownloadQueued(
                        workId = workId.toString(),
                        episodeLocalId = episodeLocalId,
                    )
                }
                isCancellable -> {
                    val workId = episodeRepository.getEpisodeDownloadWorkId(episodeLocalId)
                    if (workId != null) {
                        episodeDownloadManager.cancelDownload(workId)
                        episodeRepository.clearEpisodeDownload(episodeLocalId)
                    }
                }
            }
        }
    }

    private fun observePlaybackPositionForPositionCache() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)

                val episodeId = controller.currentMediaItem?.mediaId
                // val duration = controller.duration
                val currentTime = controller.currentPosition

                episodeId?.let {
                    saveEpisodePlaybackProgress(episodeId, currentTime)
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
                            if (_uiState.value.currentTime != currentPosition && !isSliderValueChangePending) {
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

    private fun observeBook(bookId: Identifier) {
        bookJob?.cancel()

        bookJob = viewModelScope.launch {
            bookRepository.getBook(bookId).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update {
                            it.copy(isLoading = true)
                        }
                    }
                    is Result.Success -> {
                        val book = result.data

                        if (book == null) {
                            navigateBackWithMessage("Invalid book identifier")
                        } else {
                            if (controller.currentMediaItem == null || currentBookId != bookId) {
                                val mediaItems = book.episodesAsMediaItems()
                                val currentIndex = book.recentEpisodeIndex
                                val currentTime = book.recentEpisode.progress.time

                                controller.stop()
                                controller.clearMediaItems()
                                controller.setMediaItems(mediaItems, currentIndex, currentTime)
                                controller.prepare()

                                _uiState.updateSliderValueAndCurrentTime(currentTime.toFloat())
                            }

                            _uiState.update {
                                it.copy(
                                    book = book,
                                    isLoading = false,
                                    isBookmarkButtonVisible = true,
                                    isDescriptionButtonVisible = !book.description.isNullOrBlank(),
                                )
                            }

                            currentBookId = bookId
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateBackWithMessage(message: String) {
        viewModelScope.launch {
            _uiEvents.emit(PlayerUiEvent.NavigateBack)
            _uiEvents.emit(PlayerUiEvent.ShowMessage(message))
            onMiniPlayerDismissButtonClicked()
        }
    }

    private fun Book.episodesAsMediaItems(): List<MediaItem> {
        return episodes.map { episode ->
            MediaItem.Builder()
                .setMediaId(episode.title)
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
        val sessionToken = SessionToken(context, ComponentName(context, PlayerPlaybackService::class.java))
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

    private fun observeAutoPlayTrigger() {
        viewModelScope.launch {
            autoPlayTrigger.collect {
                if (settingsRepository.getAutoPlaySetting().first()) {
                    getControllerIfInitialized()?.let {
                        if (!it.isLoading && !it.isPlaying) {
                            it.play()
                        }
                    }
                }
            }
        }
    }

    private fun observeAutoDownload() {
        viewModelScope.launch {
            combine(
                isAutoDownloadEnabledState,
                _uiState
                    .map {
                        val episodeIndex = getControllerIfInitialized()?.currentMediaItemIndex ?: INDEX_UNSET
                        val episodeId = _uiState.getEpisodeByIndex(episodeIndex)?.id?.localId
                        episodeId to it.isPlaying
                    }
                    .distinctUntilChanged()
            ) { isAutoDownloadEnabled, (episodeId, isPlaying) ->
                Triple(isAutoDownloadEnabled, episodeId, isPlaying)
            }
                .filter { (isAutoDownloadEnabled, episodeId, isPlaying) ->
                    isAutoDownloadEnabled && episodeId != null && isPlaying
                }
                .map { it.second!! }
                .scan(emptySet<String>()) { processedEpisodes, episodeId ->
                    if (episodeId in processedEpisodes) processedEpisodes
                    else {
                        _uiState.getEpisodeByLocalId(episodeId)?.let {
                            downloadEpisode(it)
                        }
                        processedEpisodes + episodeId
                    }
                }.collect()

        }
    }

    private fun observePlaybackSource() {
        viewModelScope.launch {
            combine(
                isConnectedToInternetFlow(),
                _uiState
            ) { isConnected, state ->
                if (state.book != null) {
                    val episode = state.book.recentEpisode
                    val localPath = state.book.recentEpisode.localPath
                    val localFile = localPath?.let { File(it) }

                    when {
                        localFile != null && localFile.exists() -> localFile.toUri()
                        isConnected -> Uri.parse(episode.url)
                        else -> null
                    }
                } else {
                    null
                }
            }
                .distinctUntilChanged()
                .collect { newUri ->
                    getControllerIfInitialized()?.let { controller ->
                        if (newUri == null) {
                            if (controller.isPlaying) controller.pause()
                        } else {
                            val currentUri = controller.currentMediaItem?.localConfiguration?.uri

                            if (currentUri != newUri) {
                                val mediaItem = controller.currentMediaItem
                                val index = controller.currentMediaItemIndex

                                if (mediaItem != null && index != INDEX_UNSET) {
                                    val updatedMediaItem = mediaItem.buildUpon().setUri(newUri).build()
                                    val position = controller.currentPosition

                                    controller.replaceMediaItem(index, updatedMediaItem)
                                    controller.seekTo(index, position)
                                    controller.prepare()
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun sendMessageEvent(message: String) {
        viewModelScope.launch {
            _uiEvents.emit(PlayerUiEvent.ShowMessage(message))
        }
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

    private fun getControllerIfInitialized() =
        if (this@PlayerViewModel::controller.isInitialized) controller else null

    private fun MutableStateFlow<PlayerUiState>.getCurrentEpisodeCachedPlaybackPosition(): Long {
        return value.book?.recentEpisode?.progress?.time ?: 0
    }

    private fun MutableStateFlow<PlayerUiState>.getEpisodeCachedPlaybackPositionByIndex(index: Int): Long {
        return value.book?.episodes?.getOrNull(index)?.progress?.time ?: 0
    }

    private fun MutableStateFlow<PlayerUiState>.getEpisodeByIndex(index: Int): Episode? {
        return value.book?.episodes?.getOrNull(index)
    }

    private fun MutableStateFlow<PlayerUiState>.getEpisodeByExternalId(id: String?): Episode? {
        return value.book?.episodes?.find { it.id.externalId == id }
    }

    private fun MutableStateFlow<PlayerUiState>.getEpisodeByLocalId(id: String?): Episode? {
        return value.book?.episodes?.find { it.id.localId == id }
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
                    saveEpisodePlaybackProgress(
                        externalEpisodeId = player.currentMediaItem?.mediaId,
                        currentTime = player.currentPosition
                    )
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                val prevEpisode = _uiState.getEpisodeByIndex(controller.previousMediaItemIndex)

                if (prevEpisode != null) {
                    viewModelScope.launch {
                        saveEpisodePlaybackProgress(
                            externalEpisodeId = prevEpisode.id.externalId,
                            currentTime = prevEpisode.duration,
                        )
                    }
                }
            }

            val newEpisode = _uiState.getEpisodeByExternalId(mediaItem?.mediaId)

            if (newEpisode?.hasError == true) {
                onEpisodeClick(controller.nextMediaItemIndex)
            } else if (newEpisode != null) {
                _uiState.update {
                    it.copy(book = it.book?.copy(recentEpisode = newEpisode))
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

        override fun onPlayerError(error: PlaybackException) {
            val message = when (val cause = error.cause) {
                is HttpDataSource.InvalidResponseCodeException -> "Server error (${cause.responseCode})"
                else -> "Playback failed: ${error.message}"
            }

            navigateBackWithMessage(message)
        }
    }

    private enum class RewindAction {
        BACKWARD, FORWARD;
    }
}