package org.n3gbx.whisper.feature.library

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.n3gbx.whisper.MainDispatcherRule
import org.n3gbx.whisper.core.common.GetString
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.BooksType
import org.n3gbx.whisper.model.Episode
import org.n3gbx.whisper.model.EpisodeProgress
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import java.time.LocalDateTime

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockBookRepository: BookRepository = mockk()
    private val mockGetString: GetString = mockk()

    private lateinit var sut: LibraryViewModel

    private fun createSut() {
        sut = LibraryViewModel(mockBookRepository, mockGetString)
    }

    private fun makeBook(localId: String = "local-1"): Book {
        val bookId = Identifier(localId = localId, externalId = "ext-$localId")
        val episodeId = Identifier(localId = "ep-$localId", externalId = "ext-ep-$localId")
        val episode = Episode(
            id = episodeId,
            bookId = bookId,
            title = "Episode 1",
            url = "https://example.com/episode.mp3",
            duration = 3600000L,
            progress = EpisodeProgress(
                episodeId = episodeId,
                time = 0L,
                lastUpdatedAt = LocalDateTime.now()
            ),
            download = null,
            localPath = null,
        )
        return Book(
            id = bookId,
            title = "Book $localId",
            author = "Author",
            narrator = null,
            coverUrl = null,
            description = null,
            isBookmarked = false,
            recentEpisode = episode,
            episodes = listOf(episode),
        )
    }

    @Test
    fun givenLoadingResult_whenViewModelCreated_thenIsLoadingIsTrue() = runTest {
        every { mockBookRepository.getBooks(booksType = BooksType.STARTED) } returns flowOf(Result.Loading())

        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.value.isLoading shouldBe true
    }

    @Test
    fun givenSuccessResult_whenViewModelCreated_thenBooksAreUpdatedAndIsLoadingIsFalse() = runTest {
        val stubBooks = listOf(makeBook())
        every { mockBookRepository.getBooks(booksType = BooksType.STARTED) } returns flowOf(Result.Success(stubBooks))

        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        with(sut.uiState.value) {
            books shouldBe stubBooks
            isLoading shouldBe false
        }
    }

    @Test
    fun givenErrorResult_whenViewModelCreated_thenIsLoadingIsFalseAndShowMessageEventEmitted() = runTest {
        val stubErrorMessage = "Something went wrong"
        every { mockBookRepository.getBooks(booksType = BooksType.STARTED) } returns flowOf(Result.Error(RuntimeException()))
        every { mockGetString(any()) } returns stubErrorMessage

        createSut()

        sut.uiEvents.test {
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            (awaitItem() as LibraryUiEvent.ShowMessage).message shouldBe stubErrorMessage
            cancelAndIgnoreRemainingEvents()
        }
        sut.uiState.value.isLoading shouldBe false
    }

    @Test
    fun givenInitialState_whenLibraryTabAtIndex1Clicked_thenSelectedBooksTypeIsFinished() = runTest {
        every { mockBookRepository.getBooks(booksType = BooksType.STARTED) } returns flowOf(Result.Loading())
        every { mockBookRepository.getBooks(booksType = BooksType.FINISHED) } returns flowOf(Result.Loading())
        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onLibraryTabClick(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        with(sut.uiState.value) {
            selectedBooksTypeIndex shouldBe 1
            selectedBooksType shouldBe BooksType.FINISHED
        }
    }

    @Test
    fun givenBooksLoaded_whenLibraryTabClicked_thenBooksAreClearedBeforeNewResultsArrive() = runTest {
        val stubBooks = listOf(makeBook())
        every { mockBookRepository.getBooks(booksType = BooksType.STARTED) } returns flowOf(Result.Success(stubBooks))
        every { mockBookRepository.getBooks(booksType = BooksType.FINISHED) } returns flowOf(Result.Loading())
        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onLibraryTabClick(1)

        sut.uiState.value.books shouldBe emptyList()
    }

    @Test
    fun givenSuccessResultForFinishedType_whenLibraryTabAtIndex1Clicked_thenFinishedBooksAreLoaded() = runTest {
        val stubStartedBooks = listOf(makeBook("started-1"))
        val stubFinishedBooks = listOf(makeBook("finished-1"), makeBook("finished-2"))
        every { mockBookRepository.getBooks(booksType = BooksType.STARTED) } returns flowOf(Result.Success(stubStartedBooks))
        every { mockBookRepository.getBooks(booksType = BooksType.FINISHED) } returns flowOf(Result.Success(stubFinishedBooks))
        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onLibraryTabClick(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.value.books shouldBe stubFinishedBooks
    }

    @Test
    fun givenErrorResultForNewBooksType_whenLibraryTabAtIndex1Clicked_thenIsLoadingIsFalseAndShowMessageEventEmitted() = runTest {
        val stubErrorMessage = "Something went wrong"
        every { mockBookRepository.getBooks(booksType = BooksType.STARTED) } returns flowOf(Result.Loading())
        every { mockBookRepository.getBooks(booksType = BooksType.FINISHED) } returns flowOf(Result.Error(RuntimeException()))
        every { mockGetString(any()) } returns stubErrorMessage
        createSut()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiEvents.test {
            sut.onLibraryTabClick(1)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            (awaitItem() as LibraryUiEvent.ShowMessage).message shouldBe stubErrorMessage
            cancelAndIgnoreRemainingEvents()
        }
        sut.uiState.value.isLoading shouldBe false
    }

    @Test
    fun givenBookId_whenBookmarkButtonClicked_thenUpdateBookBookmarkIsCalledOnceWithBookId() = runTest {
        val dummyBookId = Identifier(localId = "book-1", externalId = "ext-book-1")
        every { mockBookRepository.getBooks(booksType = BooksType.STARTED) } returns flowOf(Result.Loading())
        coEvery { mockBookRepository.updateBookBookmark(dummyBookId) } returns Unit
        createSut()

        sut.onBookmarkButtonClick(dummyBookId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockBookRepository.updateBookBookmark(dummyBookId) }
    }
}
