package org.n3gbx.whisper.feature.downloads

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.n3gbx.whisper.MainDispatcherRule
import org.n3gbx.whisper.data.EpisodeRepository
import org.n3gbx.whisper.domain.DeleteEpisodeCacheUseCase
import org.n3gbx.whisper.domain.DeleteEpisodesCacheUseCase
import org.n3gbx.whisper.model.Episode
import org.n3gbx.whisper.model.EpisodeProgress
import org.n3gbx.whisper.model.Identifier
import java.time.LocalDateTime

class DownloadsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockEpisodeRepository: EpisodeRepository = mockk()
    private val mockDeleteEpisodeCache: DeleteEpisodeCacheUseCase = mockk()
    private val mockDeleteEpisodesCache: DeleteEpisodesCacheUseCase = mockk()

    private lateinit var sut: DownloadsViewModel

    @Before
    fun setUp() {
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(emptyList())
    }

    private fun createSut() {
        sut = DownloadsViewModel(mockEpisodeRepository, mockDeleteEpisodeCache, mockDeleteEpisodesCache)
    }

    private fun makeEpisode(localId: String = "ep-1", localPath: String? = null): Episode {
        val episodeId = Identifier(localId = localId, externalId = "ext-$localId")
        return Episode(
            id = episodeId,
            bookId = Identifier(localId = "book-1", externalId = "ext-book-1"),
            title = "Episode $localId",
            url = "https://example.com/$localId.mp3",
            duration = 3600000L,
            progress = EpisodeProgress(
                episodeId = episodeId,
                time = 0L,
                lastUpdatedAt = LocalDateTime.now()
            ),
            download = null,
            localPath = localPath,
        )
    }

    @Test
    fun givenDownloadedEpisodes_whenViewModelCreated_thenEpisodesAreEmittedAndLoadingIsFalse() = runTest {
        val stubEpisodes = listOf(makeEpisode("ep-1"), makeEpisode("ep-2"))
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(stubEpisodes)

        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        with(sut.uiState.value) {
            isLoading shouldBe false
            episodes.map { it.first } shouldBe stubEpisodes
        }
    }

    @Test
    fun givenNoDownloadedEpisodes_whenViewModelCreated_thenEpisodesIsEmptyAndLoadingIsFalse() = runTest {
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(emptyList())

        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        with(sut.uiState.value) {
            isLoading shouldBe false
            episodes shouldBe emptyList()
        }
    }

    @Test
    fun givenEpisodeWithNoLocalPath_whenViewModelCreated_thenFileSizeIsUnknown() = runTest {
        val stubEpisode = makeEpisode(localPath = null)
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(listOf(stubEpisode))

        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.value.episodes.first().second shouldBe "??? MB"
    }

    @Test
    fun givenEpisodesLoaded_whenCheckingClearAllButtonVisibility_thenItIsVisible() = runTest {
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(listOf(makeEpisode()))

        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.value.isClearAllButtonVisible shouldBe true
    }

    @Test
    fun givenNoEpisodesLoaded_whenCheckingClearAllButtonVisibility_thenItIsNotVisible() = runTest {
        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.value.isClearAllButtonVisible shouldBe false
    }

    @Test
    fun givenInitialState_whenBackButtonClicked_thenNavigateBackEventIsEmitted() = runTest {
        createSut()

        sut.uiEvents.test {
            sut.onBackButtonClick()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() shouldBe DownloadsUiEvent.NavigateBack
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenInitialState_whenDeleteAllClicked_thenDeleteAllDialogIsShown() = runTest {
        createSut()

        sut.onDeleteAllClick()

        sut.uiState.value.deleteDialog shouldBe DownloadsUiState.Dialog.DeleteAllDialog
    }

    @Test
    fun givenDeleteAllDialog_whenConfirmedAndDeleteSucceeds_thenDialogIsHiddenAndSuccessMessageIsEmitted() = runTest {
        coEvery { mockDeleteEpisodesCache() } returns true
        createSut()
        sut.onDeleteAllClick()

        sut.uiEvents.test {
            sut.onDeleteAllConfirm()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            sut.uiState.value.deleteDialog shouldBe null
            (awaitItem() as DownloadsUiEvent.ShowMessage).message shouldBe "Deleted successfully"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenDeleteAllDialog_whenConfirmedAndDeleteFails_thenDialogIsHiddenAndFailureMessageIsEmitted() = runTest {
        coEvery { mockDeleteEpisodesCache() } returns false
        createSut()
        sut.onDeleteAllClick()

        sut.uiEvents.test {
            sut.onDeleteAllConfirm()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            sut.uiState.value.deleteDialog shouldBe null
            (awaitItem() as DownloadsUiEvent.ShowMessage).message shouldBe "Delete failed"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenDeleteAllDialog_whenConfirmedAndDeleteSucceeds_thenDeleteEpisodesCacheIsCalledOnce() = runTest {
        coEvery { mockDeleteEpisodesCache() } returns true
        createSut()
        sut.onDeleteAllClick()

        sut.uiEvents.test {
            sut.onDeleteAllConfirm()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mockDeleteEpisodesCache() }
    }

    @Test
    fun givenEpisode_whenDeleteDownloadClicked_thenDeleteDownloadDialogIsShownForThatEpisode() = runTest {
        val stubEpisode = makeEpisode()
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(listOf(stubEpisode))
        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onDownloadDeleteClick(stubEpisode)

        sut.uiState.value.deleteDialog shouldBe DownloadsUiState.Dialog.DeleteDownloadDialog(stubEpisode)
    }

    @Test
    fun givenDeleteDownloadDialog_whenConfirmedAndDeleteSucceeds_thenDialogIsHiddenAndSuccessMessageIsEmitted() = runTest {
        val stubEpisode = makeEpisode()
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(listOf(stubEpisode))
        coEvery { mockDeleteEpisodeCache(stubEpisode) } returns true
        createSut()
        sut.onDownloadDeleteClick(stubEpisode)

        sut.uiEvents.test {
            sut.onDownloadDeleteConfirm(stubEpisode)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            sut.uiState.value.deleteDialog shouldBe null
            (awaitItem() as DownloadsUiEvent.ShowMessage).message shouldBe "Deleted successfully"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenDeleteDownloadDialog_whenConfirmedAndDeleteFails_thenDialogIsHiddenAndFailureMessageIsEmitted() = runTest {
        val stubEpisode = makeEpisode()
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(listOf(stubEpisode))
        coEvery { mockDeleteEpisodeCache(stubEpisode) } returns false
        createSut()
        sut.onDownloadDeleteClick(stubEpisode)

        sut.uiEvents.test {
            sut.onDownloadDeleteConfirm(stubEpisode)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            sut.uiState.value.deleteDialog shouldBe null
            (awaitItem() as DownloadsUiEvent.ShowMessage).message shouldBe "Delete failed"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenDeleteDownloadDialog_whenConfirmedAndDeleteSucceeds_thenDeleteEpisodeCacheIsCalledOnceWithEpisode() = runTest {
        val stubEpisode = makeEpisode()
        every { mockEpisodeRepository.getDownloadedEpisodes() } returns flowOf(listOf(stubEpisode))
        coEvery { mockDeleteEpisodeCache(stubEpisode) } returns true
        createSut()
        sut.onDownloadDeleteClick(stubEpisode)

        sut.uiEvents.test {
            sut.onDownloadDeleteConfirm(stubEpisode)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mockDeleteEpisodeCache(stubEpisode) }
    }

    @Test
    fun givenDialogShown_whenDismissed_thenDialogIsHidden() = runTest {
        createSut()
        sut.onDeleteAllClick()
        sut.uiState.value.deleteDialog shouldNotBe null

        sut.onDialogDismiss()

        sut.uiState.value.deleteDialog shouldBe null
    }
}
