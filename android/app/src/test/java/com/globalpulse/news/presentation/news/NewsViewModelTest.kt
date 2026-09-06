package com.globalpulse.news.presentation.news

import com.globalpulse.news.domain.model.DataStatus
import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.domain.model.NewsItem
import com.globalpulse.news.domain.model.VerificationStatus
import com.globalpulse.news.domain.repository.NewsRepository
import com.globalpulse.news.domain.repository.NewsResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleItem(id: String = "1") = NewsItem(
        id = id,
        headline = "Sample headline",
        summary = null,
        source = "Test Source",
        sourceUrl = "https://example.com",
        publishedAt = OffsetDateTime.now(),
        dataStatus = DataStatus.DELAYED,
        tickers = emptyList(),
        verificationStatus = VerificationStatus.SINGLE_SOURCE,
        corroboratingSources = emptyList(),
        potentialImpact = emptyList()
    )

    @Test
    fun `initial state loads news and reflects success`() = runTest {
        val fakeRepo = FakeNewsRepository(NewsResult.Success(listOf(sampleItem())))
        val viewModel = NewsViewModel(fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.items).hasSize(1)
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `error result surfaces as an error message, not a crash`() = runTest {
        val fakeRepo = FakeNewsRepository(NewsResult.Error("الخادم غير متاح"))
        val viewModel = NewsViewModel(fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("الخادم غير متاح")
        assertThat(state.items).isEmpty()
    }

    @Test
    fun `selecting a market re-fetches with that market`() = runTest {
        val fakeRepo = FakeNewsRepository(NewsResult.Success(listOf(sampleItem())))
        val viewModel = NewsViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onMarketSelected(Market.EGX)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeRepo.lastRequestedMarket).isEqualTo(Market.EGX)
        assertThat(viewModel.uiState.value.selectedMarket).isEqualTo(Market.EGX)
    }

    @Test
    fun `selecting the same market twice does not trigger a redundant fetch`() = runTest {
        val fakeRepo = FakeNewsRepository(NewsResult.Success(listOf(sampleItem())))
        val viewModel = NewsViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val callsBefore = fakeRepo.callCount
        viewModel.onMarketSelected(null) // already null by default
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeRepo.callCount).isEqualTo(callsBefore)
    }

    @Test
    fun `refresh triggers a new fetch with the current market`() = runTest {
        val fakeRepo = FakeNewsRepository(NewsResult.Success(listOf(sampleItem())))
        val viewModel = NewsViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val callsBefore = fakeRepo.callCount
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeRepo.callCount).isEqualTo(callsBefore + 1)
    }

    private class FakeNewsRepository(
        private val result: NewsResult
    ) : NewsRepository {
        var callCount = 0
            private set
        var lastRequestedMarket: Market? = null
            private set

        override suspend fun getTrendingNews(market: Market?, includeGeneral: Boolean): NewsResult {
            callCount++
            lastRequestedMarket = market
            return result
        }
    }
}
