package org.n3gbx.whisper.feature.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.DownloadState.QUEUED
import org.n3gbx.whisper.model.DownloadState.PROGRESSING
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
import org.n3gbx.whisper.model.PlaybackSource
import org.n3gbx.whisper.utils.connectionTypeFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
    private val episodeRepository: EpisodeRepository,
    private val episodeDownloadManager: EpisodeDownloadManager,
    private val isConnectedToInternetFlow: IsConnectedToInternet,
) : ViewModel() {

    private lateinit var controller: MediaController
    private var currentBookId: Identifier? = null
    private var bookJob: Job? = null
    private var pendingSliderValue: Float? = null
    private var isSliderValueChangePending: Boolean = false
    private var isAutoPlayPending: Boolean = false

    private val rewindEvents = MutableSharedFlow<RewindEvent>()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<PlayerUiEvent>()
    val uiEvents: SharedFlow<PlayerUiEvent> = _uiEvents.asSharedFlow()

    /**
     * Whether auto download is enabled by settings constraints
     */
    private val isAutoDownloadEnabledState =
        combine(
            context.connectionTypeFlow(),
            settingsRepository.getAutoDownloadSetting(),
            settingsRepository.getDownloadWifiOnlySetting(),
        ) { connectionType, isEnabled, isWifiOnly ->
            if (isWifiOnly) connectionType == WIFI && isEnabled
            else connectionType != NONE && isEnabled
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Whether download is enabled by settings constraints
     */
    private val isDownloadEnabledState =
        combine(
            context.connectionTypeFlow(),
            settingsRepository.getDownloadWifiOnlySetting(),
        ) { connectionType, isWifiOnly ->
            if (isWifiOnly) connectionType == WIFI
            else connectionType != NONE
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        initController()
        observeAutoDownload()
        observePlaybackSource()
    }

    fun setBook(bookId: Identifier?) {
        if (::controller.isInitialized && bookId != null && currentBookId != bookId) {
            observeBook(bookId)
        }
    }

    /**
     * Update uiState playback speed, send uiEvent message, and set controller playback speed
     */
    fun onSpeedOptionChange(speedOption: SpeedOption) {
        _uiState.updateSpeedOption(speedOption)
        _uiEvents.sendMessage("Speed set: ${speedOption.label}")

        controller.setPlaybackSpeed(speedOption.value)
    }

    /**
     * Update uiState sleep timer, send uiEvent message, and send custom sleep timer command to controller
     */
    fun onSleepTimerOptionChange(sleepTimerOption: SleepTimerOption?) {
        _uiState.updateSleepTimer(sleepTimerOption)

        if (sleepTimerOption != null) {
            val commandExtras = Bundle().apply { putLong("duration", sleepTimerOption.value) }
            val customCommand = SessionCommand(START_SLEEP_TIMER.name, commandExtras)
            controller.sendCustomCommand(customCommand, Bundle.EMPTY)
            _uiEvents.sendMessage("Sleep timer set: ${sleepTimerOption.label}")
        } else {
            val customCommand = SessionCommand(CANCEL_SLEEP_TIMER.name, Bundle.EMPTY)
            controller.sendCustomCommand(customCommand, Bundle.EMPTY)
            _uiEvents.sendMessage("Sleep timer cancelled")
        }
    }

    /**
     * Update uiState description visibility to true
     */
    fun onDescriptionButtonClick() {
        _uiState.updateDescriptionVisibility(true)
    }

    /**
     * Update uiState description visibility to false
     */
    fun onDescriptionDismiss() {
        _uiState.updateDescriptionVisibility(false)
    }

    /**
     * Call controller play/pause methods depending on controller playback state
     */
    fun onPlayPauseButtonClick() {
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    /**
     * Stop controller, clear media items, nullify book, and reset uiState
     */
    fun onMiniPlayerDismiss() {
        controller.pause()
        controller.stop()
        controller.clearMediaItems()

        currentBookId = null
        bookJob?.cancel()

        _uiState.update { PlayerUiState() }
    }

    /**
     * Send Backward rewind local event
     */
    fun onRewindBackwardButtonClick() {
        viewModelScope.launch {
            rewindEvents.emit(RewindEvent.Backward)
        }
    }

    /**
     * Send Forward rewind local event
     */
    fun onRewindForwardButtonClick() {
        viewModelScope.launch {
            rewindEvents.emit(RewindEvent.Forward)
        }
    }

    /**
     * Update uiState and pending slider value
     */
    fun onSliderValueChange(value: Float) {
        isSliderValueChangePending = true
        pendingSliderValue = value

        _uiState.updateSliderValue(value)
    }

    /**
     * Update controller position with pending slider value if not null
     */
    fun onSliderValueChangeFinish() {
        val value = pendingSliderValue ?: return
        pendingSliderValue = null
        isSliderValueChangePending = false

        controller.seekTo(value.toLong())
    }

    /**
     * Update controller position to either start or cached playback position depending on episode index and playback state
     */
    fun onEpisodeClick(index: Int) {
        val isLoading = _uiState.value.isLoading
        val isBuffering = _uiState.value.isBuffering
        val isSameEpisode = _uiState.value.book?.recentEpisodeIndex == index

        if (isLoading || isBuffering) return

        val isEpisodeFinished = _uiState.getEpisodeByIndex(index)?.isFinished == true

        if (isEpisodeFinished && isSameEpisode) {
            controller.seekTo(index, 0)
        } else if (!isSameEpisode) {
            val time = _uiState.getEpisodeCachedPlaybackPositionByIndex(index)
            controller.seekTo(index, time)
        }
    }

    /**
     * Download episode if download is enabled by settings constraints otherwise send uiEvent message
     */
    fun onEpisodeDownloadClick(index: Int) {
        if (isDownloadEnabledState.value) {
            _uiState.getEpisodeByIndex(index)?.let {
                downloadEpisode(it)
            }
        } else {
            _uiEvents.sendMessage("Download is disabled by settings constraints")
        }
    }

    /**
     * Update database book bookmark if currentBookId is not null
     */
    fun onBookmarkButtonClick() {
        currentBookId?.let { bookId ->
            viewModelScope.launch {
                bookRepository.updateBookBookmark(bookId)
            }
        }
    }

    /**
     * Save episode playback progress to database if currentBookId is not null
     */
    private suspend fun saveEpisodePlaybackProgress(externalEpisodeId: String?, currentTime: Long) {
        currentBookId?.let { bookId ->
            if (currentTime != C.TIME_UNSET && externalEpisodeId != null) {
                episodeRepository.updateEpisodeProgress(
                    bookExternalId = bookId.externalId,
                    episodeExternalId = externalEpisodeId,
                    currentTime = currentTime,
                )
            }
        }
    }

    /**
     * Download episode to cache
     */
    private fun downloadEpisode(episode: Episode) {
        viewModelScope.launch {
            val isCancellable = episode.download?.state == QUEUED || episode.download?.state == PROGRESSING
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

    /**
     * Save episode playback progress every 30s
     */
    private fun observePlaybackPositionForProgressCache() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)

                val episodeId = controller.currentMediaItem?.mediaId
                val currentTime = controller.currentPosition

                saveEpisodePlaybackProgress(episodeId, currentTime)
            }
        }
    }

    /**
     * Update uiState slider value and current time every 250ms if distinct
     */
    private fun observePlaybackPositionForStateUpdate() {
        viewModelScope.launch {
            while (isActive) {
                delay(250)

                val position = controller.currentPosition
                if (!isSliderValueChangePending && _uiState.value.currentTime != position) {
                    _uiState.updateSliderValueAndCurrentTime(position.toFloat())
                }
            }
        }
    }

    /**
     * Update controller position with accumulated rewind time
     */
    private fun observeRewindActionsForPlaybackPositionUpdate() {
        viewModelScope.launch {
            var accumulatedDelta = 0L
            rewindEvents
                .onEach { accumulatedDelta += it.rewindTimeMs }
                .debounce(300)
                .collect {
                    val newPosition = when (it) {
                        RewindEvent.Backward -> controller.currentPosition - accumulatedDelta
                        RewindEvent.Forward -> controller.currentPosition + accumulatedDelta
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
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Result.Success -> {
                        val book = result.data

                        if (book == null) {
                            navigateBackWithUiMessage("Invalid book identifier")
                        } else {
                            if (controller.currentMediaItem == null || currentBookId != bookId) {
                                updateIsAutoPlayPending(true)

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

    private fun Book.episodesAsMediaItems(): List<MediaItem> {
        return episodes.map { episode ->
            MediaItem.Builder()
                .setMediaId(episode.title)
                .setUri(episode.localPath ?: episode.url)
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

        Timber.tag(LOG_TAG).d("Init controller started: sessionToken=%s", sessionToken.uid)

        controllerFuture.addListener(
            {
                try {
                    controller = controllerFuture.get()
                    controller.addListener(ControllerListener())

                    Timber.tag(LOG_TAG).d("Init controller finished")

                    observePlaybackPositionForProgressCache()
                    observePlaybackPositionForStateUpdate()
                    observeRewindActionsForPlaybackPositionUpdate()
                } catch (e: Exception) {
                    Timber.tag(LOG_TAG).d("Init controller failed %s", e.message)
                    navigateBackWithUiMessage("Error while initialising player")
                }
            },
            ContextCompat.getMainExecutor(context)
        )
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

                    if (localFile != null && localFile.exists()) {
                        PlaybackSource.Resolved(localFile.toUri())
                    } else if (isConnected) {
                        PlaybackSource.Resolved(Uri.parse(episode.url))
                    } else {
                        PlaybackSource.Unresolved
                    }
                } else {
                    null
                }
            }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { source ->
                    getControllerIfInitialized()?.let { controller ->
                        if (source == PlaybackSource.Unresolved) {
                            if (controller.isPlaying) controller.pause()
                        } else if (source is PlaybackSource.Resolved) {
                            val newUri = source.uri
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

                                    Timber.tag(LOG_TAG).d("observePlaybackSource: index=%s, position=%s, uri:%s", index, position, newUri)
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun updateIsAutoPlayPending(value: Boolean) {
        this.isAutoPlayPending = value
    }

    private fun navigateBackWithUiMessage(message: String) {
        viewModelScope.launch {
            _uiEvents.emit(PlayerUiEvent.NavigateBack)
            _uiEvents.emit(PlayerUiEvent.ShowMessage(message))
            onMiniPlayerDismiss()
        }
    }

    private fun MutableSharedFlow<PlayerUiEvent>.sendMessage(message: String) {
        viewModelScope.launch {
            emit(PlayerUiEvent.ShowMessage(message))
        }
    }

    private fun MutableStateFlow<PlayerUiState>.updateDuration(value: Long) {
        Timber.tag(LOG_TAG).d("updateDuration: %s", value)
        update { it.copy(duration = value) }
    }

    private fun MutableStateFlow<PlayerUiState>.updateSliderValue(value: Float) {
        Timber.tag(LOG_TAG).d("updateSliderValue: %s", value)
        update { it.copy(sliderValue = value) }
    }

    private fun MutableStateFlow<PlayerUiState>.updateSliderValueAndCurrentTime(value: Float) {
        Timber.tag(LOG_TAG).d("updateSliderValueAndCurrentTime: %s", value)
        update { it.copy(sliderValue = value, currentTime = value.toLong()) }
    }

    private fun MutableStateFlow<PlayerUiState>.updateSpeedOption(value: SpeedOption) {
        Timber.tag(LOG_TAG).d("updateSpeedOption: %s", value)
        update { it.copy(selectedSpeedOption = value) }
    }

    private fun MutableStateFlow<PlayerUiState>.updateSleepTimer(value: SleepTimerOption?) {
        Timber.tag(LOG_TAG).d("updateSleepTimer: %s", value)
        update { it.copy(selectedSleepTimerOption = value) }
    }

    private fun MutableStateFlow<PlayerUiState>.updateDescriptionVisibility(value: Boolean) {
        Timber.tag(LOG_TAG).d("updateDescriptionVisibility: %s", value)
        _uiState.update { it.copy(showDescription = value) }
    }

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

    private fun getControllerIfInitialized() =
        if (this@PlayerViewModel::controller.isInitialized) controller else null

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

        /**
         * Update uiState duration when playback is ready
         */
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                controller.duration.also { duration ->
                    if (duration != C.TIME_UNSET) {
                        _uiState.updateDuration(duration)
                    }
                }

                viewModelScope.launch {
                    if (settingsRepository.getAutoPlaySetting().first()) {
                        if (!controller.isPlaying && isAutoPlayPending) {
                            controller.play()
                            updateIsAutoPlayPending(false)
                        }
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Timber.tag(LOG_TAG).d("onMediaItemTransition: mediaItemId=%s, reason=%s", mediaItem?.mediaId, reason)

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
            Timber.tag(LOG_TAG).d("onIsLoadingChanged: %s", isLoading)
            _uiState.update {
                it.copy(isBuffering = isLoading)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.tag(LOG_TAG).d("onIsPlayingChanged: %s", isPlaying)
            _uiState.update {
                it.copy(isPlaying = isPlaying)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.tag(LOG_TAG).d("onPlayerError: message=%s", error.message)

            val message = when (val cause = error.cause) {
                is HttpDataSource.InvalidResponseCodeException -> "Server error (${cause.responseCode})"
                else -> "Playback failed: ${error.message}"
            }

            navigateBackWithUiMessage(message)
        }
    }

    private sealed interface RewindEvent {
        val rewindTimeMs: Long get() = 10_000

        data object Forward : RewindEvent
        data object Backward : RewindEvent
    }

    companion object {
        private const val LOG_TAG = "PlayerViewModel"
    }
}