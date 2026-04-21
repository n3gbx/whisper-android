package org.n3gbx.whisper.data.common

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.n3gbx.whisper.model.Result

class NetworkBoundResourceTest {

    private val sut = NetworkBoundResource()

    private val mockQuery = mockk<suspend () -> Flow<String>>()
    private val mockFetch = mockk<suspend () -> String>()

    private var capturedFetchResult: String? = null
    private var capturedCurrentData: String? = null
    private val captureSaveFetched: suspend (String, String?) -> Unit = { fetchResult, currentData ->
        capturedFetchResult = fetchResult
        capturedCurrentData = currentData
    }
    private val throwSaveFetched: suspend (String, String?) -> Unit = { _, _ ->
        throw RuntimeException("Save failed")
    }

    @Before
    fun setUp() {
        capturedFetchResult = null
        capturedCurrentData = null
    }

    @Test
    fun givenQueryReturnsData_whenShouldFetchFalse_thenQueryResultIsEmittedAsSuccess() = runTest {
        coEvery { mockQuery() } returns flowOf("cachedData")

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { false }
        ).test {
            (awaitItem() as Result.Success).data shouldBe "cachedData"
            awaitComplete()
        }
    }

    @Test
    fun givenQueryReturnsData_whenShouldFetchFalse_thenFetchIsNotCalled() = runTest {
        coEvery { mockQuery() } returns flowOf("cachedData")

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { false }
        ).test {
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 0) { mockFetch() }
    }

    @Test
    fun givenQueryReturnsData_whenShouldFetchFalse_thenSaveFetchedIsNotCalled() = runTest {
        coEvery { mockQuery() } returns flowOf("cachedData")

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { false }
        ).test {
            awaitItem()
            awaitComplete()
        }

        capturedFetchResult shouldBe null
    }

    @Test
    fun givenQueryReturnsData_whenShouldFetchTrue_thenLoadingIsEmittedBeforeSuccess() = runTest {
        coEvery { mockQuery() } returns flowOf("freshData")
        coEvery { mockFetch() } returns "fetchResult"

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { true }
        ).test {
            awaitItem().shouldBeInstanceOf<Result.Loading<String>>()
            (awaitItem() as Result.Success).data shouldBe "freshData"
            awaitComplete()
        }
    }

    @Test
    fun givenQueryReturnsData_whenShouldFetchTrue_thenFetchIsCalledOnce() = runTest {
        coEvery { mockQuery() } returns flowOf("freshData")
        coEvery { mockFetch() } returns "fetchResult"

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { true }
        ).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 1) { mockFetch() }
    }

    @Test
    fun givenQueryReturnsData_whenShouldFetchTrue_thenSaveFetchedIsCalledWithFetchResultAndCurrentData() = runTest {
        coEvery { mockQuery() } returns flowOf("cachedData")
        coEvery { mockFetch() } returns "fetchResult"

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { true }
        ).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        capturedFetchResult shouldBe "fetchResult"
        capturedCurrentData shouldBe "cachedData"
    }

    @Test
    fun givenNullCurrentData_whenShouldFetchTrue_thenSaveFetchedIsCalledWithNullCurrentData() = runTest {
        coEvery { mockQuery() } returnsMany listOf(flowOf(), flowOf("freshData"))
        coEvery { mockFetch() } returns "fetchResult"

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { true }
        ).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        capturedFetchResult shouldBe "fetchResult"
        capturedCurrentData shouldBe null
    }

    @Test
    fun givenQueryReturnsMultipleItems_whenShouldFetchFalse_thenAllItemsAreEmittedAsSuccess() = runTest {
        coEvery { mockQuery() } returns flowOf("item1", "item2", "item3")

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { false }
        ).test {
            (awaitItem() as Result.Success).data shouldBe "item1"
            (awaitItem() as Result.Success).data shouldBe "item2"
            (awaitItem() as Result.Success).data shouldBe "item3"
            awaitComplete()
        }
    }

    @Test
    fun givenQueryThrows_whenInvoked_thenErrorIsEmitted() = runTest {
        val stubException = RuntimeException("Query failed")
        coEvery { mockQuery() } throws stubException

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { false }
        ).test {
            val item = awaitItem()
            item.shouldBeInstanceOf<Result.Error<String>>()
            (item as Result.Error).e shouldBe stubException
            awaitComplete()
        }
    }

    @Test
    fun givenFetchThrows_whenShouldFetchTrue_thenLoadingFollowedByErrorIsEmitted() = runTest {
        val stubException = RuntimeException("Fetch failed")
        coEvery { mockQuery() } returns flowOf("cachedData")
        coEvery { mockFetch() } throws stubException

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = captureSaveFetched,
            shouldFetch = { true }
        ).test {
            awaitItem().shouldBeInstanceOf<Result.Loading<String>>()
            val item = awaitItem()
            item.shouldBeInstanceOf<Result.Error<String>>()
            (item as Result.Error).e shouldBe stubException
            awaitComplete()
        }
    }

    @Test
    fun givenSaveFetchedThrows_whenShouldFetchTrue_thenLoadingFollowedByErrorIsEmitted() = runTest {
        coEvery { mockQuery() } returns flowOf("cachedData")
        coEvery { mockFetch() } returns "fetchResult"

        sut(
            query = mockQuery,
            fetch = mockFetch,
            saveFetched = throwSaveFetched,
            shouldFetch = { true }
        ).test {
            awaitItem().shouldBeInstanceOf<Result.Loading<String>>()
            val item = awaitItem()
            item.shouldBeInstanceOf<Result.Error<String>>()
            (item as Result.Error).e.message shouldBe "Save failed"
            awaitComplete()
        }
    }
}
