package org.n3gbx.whisper.feature.catalog

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.n3gbx.whisper.MainDispatcherRule
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.data.SettingsRepository
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.BooksSortType
import org.n3gbx.whisper.model.Episode
import org.n3gbx.whisper.model.EpisodeProgress
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockBookRepository: BookRepository = mockk()
    private val mockSettingsRepository: SettingsRepository = mockk()

    private lateinit var sut: CatalogViewModel

    @Before
    fun setUp() {
        every { mockBookRepository.getBooks(any(), any(), any(), any()) } returns flowOf(Result.Loading())
        every { mockSettingsRepository.getCatalogGridLayoutSetting() } returns flowOf(false)
    }

    private fun createSut() {
        sut = CatalogViewModel(mockBookRepository, mockSettingsRepository)
    }

    private fun makeBook(localId: String = "book-1"): Book {
        val bookId = Identifier(localId = localId, externalId = "ext-$localId")
        val episodeId = Identifier(localId = "ep-$localId", externalId = "ext-ep-$localId")
        val episode = Episode(
            id = episodeId,
            bookId = bookId,
            title = "Episode $localId",
            url = "https://example.com/$localId.mp3",
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
        createSut()
        advanceUntilIdle()

        sut.uiState.value.isLoading shouldBe true
    }

    @Test
    fun givenSuccessResult_whenViewModelCreated_thenBooksArePopulatedAndIsLoadingIsFalse() = runTest {
        val stubBooks = listOf(makeBook("book-1"), makeBook("book-2"))
        every { mockBookRepository.getBooks(any(), any(), any(), any()) } returns flowOf(Result.Success(stubBooks))

        createSut()
        advanceUntilIdle()

        with(sut.uiState.value) {
            books shouldBe stubBooks
            isLoading shouldBe false
        }
    }

    @Test
    fun givenGridLayoutEnabled_whenViewModelCreated_thenIsGridLayoutIsTrue() = runTest {
        every { mockBookRepository.getBooks(any(), any(), any(), any()) } returns flowOf(Result.Success(emptyList()))
        every { mockSettingsRepository.getCatalogGridLayoutSetting() } returns flowOf(true)

        createSut()
        advanceUntilIdle()

        sut.uiState.value.isGridLayout shouldBe true
    }

    @Test
    fun givenGridLayoutDisabled_whenViewModelCreated_thenIsGridLayoutIsFalse() = runTest {
        createSut()
        advanceUntilIdle()

        sut.uiState.value.isGridLayout shouldBe false
    }

    @Test
    fun givenSearchQuery_whenSearchQueryEntered_thenSearchQueryIsUpdatedInState() = runTest {
        createSut()

        sut.onSearchQuery("tolkien")

        sut.uiState.value.searchQuery shouldBe "tolkien"
    }

    @Test
    fun givenSearchQuery_whenSearchQueryEntered_thenGetBooksIsCalledWithThatQueryAfterDebounce() = runTest {
        createSut()
        advanceUntilIdle()

        sut.onSearchQuery("tolkien")
        advanceUntilIdle()

        verify { mockBookRepository.getBooks(query = eq("tolkien"), shouldRefresh = any(), booksType = any(), booksSortType = any()) }
    }

    @Test
    fun givenSearchQuerySet_whenSearchQueryCleared_thenSearchQueryIsNull() = runTest {
        createSut()
        sut.onSearchQuery("tolkien")

        sut.onSearchQueryClear()

        sut.uiState.value.searchQuery shouldBe null
    }

    @Test
    fun givenSearchQuerySet_whenSearchQueryCleared_thenGetBooksIsCalledWithNullQueryAfterDebounce() = runTest {
        createSut()
        advanceUntilIdle()
        sut.onSearchQuery("tolkien")
        advanceTimeBy(300)

        sut.onSearchQueryClear()
        advanceTimeBy(300)

        verify(atLeast = 1) { mockBookRepository.getBooks(query = null, shouldRefresh = any(), booksType = any(), booksSortType = any()) }
    }

    @Test
    fun givenSearchHidden_whenSearchToggled_thenIsSearchVisibleIsTrue() = runTest {
        createSut()

        sut.onSearchToggle()

        sut.uiState.value.isSearchVisible shouldBe true
    }

    @Test
    fun givenSearchVisible_whenSearchToggled_thenIsSearchVisibleIsFalse() = runTest {
        createSut()
        sut.onSearchToggle()

        sut.onSearchToggle()

        sut.uiState.value.isSearchVisible shouldBe false
    }

    @Test
    fun givenBookId_whenBookmarkButtonClicked_thenUpdateBookBookmarkIsCalledOnceWithBookId() = runTest {
        val dummyBookId = Identifier(localId = "book-1", externalId = "ext-book-1")
        coEvery { mockBookRepository.updateBookBookmark(dummyBookId) } returns Unit
        createSut()

        sut.onBookmarkButtonClick(dummyBookId)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockBookRepository.updateBookBookmark(dummyBookId) }
    }

    @Test
    fun givenInitialState_whenRefreshed_thenGetBooksIsCalledWithShouldRefreshTrue() = runTest {
        createSut()
        advanceUntilIdle()

        sut.onRefresh()
        advanceUntilIdle()

        verify(exactly = 1) { mockBookRepository.getBooks(query = any(), shouldRefresh = eq(true), booksType = any(), booksSortType = any()) }
    }

    @Test
    fun givenSortOption_whenSortOptionChanged_thenSelectedSortOptionIsUpdated() = runTest {
        createSut()

        sut.onSortOptionChange(SortOption.TITLE_ASC)
        advanceUntilIdle()

        sut.uiState.value.selectedSortOption shouldBe SortOption.TITLE_ASC
    }

    @Test
    fun givenSortOption_whenSortOptionChanged_thenGetBooksIsCalledWithThatSortType() = runTest {
        createSut()
        advanceUntilIdle()

        sut.onSortOptionChange(SortOption.TITLE_ASC)
        advanceUntilIdle()

        verify(exactly = 1) { mockBookRepository.getBooks(query = any(), shouldRefresh = any(), booksType = any(), booksSortType = eq(BooksSortType.TITLE_ASC)) }
    }

    @Test
    fun givenNullSortOption_whenSortOptionChanged_thenSelectedSortOptionIsNull() = runTest {
        createSut()
        sut.onSortOptionChange(SortOption.TITLE_ASC)
        advanceUntilIdle()

        sut.onSortOptionChange(null)
        advanceUntilIdle()

        sut.uiState.value.selectedSortOption shouldBe null
    }

    @Test
    fun givenGridLayoutDisabled_whenLayoutToggled_thenSetCatalogGridLayoutIsCalledWithTrue() = runTest {
        coEvery { mockSettingsRepository.setCatalogGridLayoutSetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onLayoutToggle()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setCatalogGridLayoutSetting(true) }
    }

    @Test
    fun givenGridLayoutEnabled_whenLayoutToggled_thenSetCatalogGridLayoutIsCalledWithFalse() = runTest {
        every { mockBookRepository.getBooks(any(), any(), any(), any()) } returns flowOf(Result.Success(emptyList()))
        every { mockSettingsRepository.getCatalogGridLayoutSetting() } returns flowOf(true)
        coEvery { mockSettingsRepository.setCatalogGridLayoutSetting(any()) } returns Unit
        createSut()
        advanceUntilIdle()

        sut.onLayoutToggle()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSettingsRepository.setCatalogGridLayoutSetting(false) }
    }
}
