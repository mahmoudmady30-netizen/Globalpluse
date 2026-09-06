package com.globalpulse.news.data.repository

import com.globalpulse.news.data.remote.NewsApi
import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.domain.repository.NewsResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * These tests spin up a real local HTTP server (MockWebServer) and exercise
 * the actual Retrofit + kotlinx.serialization parsing pipeline against
 * JSON shaped exactly like the FastAPI backend's real response — not a
 * hand-mocked repository. This is the closest thing to an integration test
 * that runs without a device/emulator or real network access.
 */
class NewsRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: NewsRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(NewsApi::class.java)
        repository = NewsRepositoryImpl(api, ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `parses a valid backend response into domain NewsItems`() = runTest {
        server.enqueue(MockResponse().setBody(SAMPLE_RESPONSE_JSON).setResponseCode(200))

        val result = repository.getTrendingNews()

        assertThat(result).isInstanceOf(NewsResult.Success::class.java)
        val items = (result as NewsResult.Success).items
        assertThat(items).hasSize(1)
        assertThat(items.first().headline).isEqualTo("Egypt central bank holds interest rates")
        assertThat(items.first().tickers.first().symbol).isEqualTo("COMI")
    }

    @Test
    fun `sends the market as a lowercase query parameter`() = runTest {
        server.enqueue(MockResponse().setBody(EMPTY_RESPONSE_JSON).setResponseCode(200))

        repository.getTrendingNews(market = Market.EGX)

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.path).contains("market=egx")
    }

    @Test
    fun `returns Error on a 503 (quota exhausted) response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"detail":"quota exceeded"}"""))

        val result = repository.getTrendingNews()

        assertThat(result).isInstanceOf(NewsResult.Error::class.java)
    }

    @Test
    fun `returns Error on malformed JSON rather than crashing`() = runTest {
        server.enqueue(MockResponse().setBody("not valid json").setResponseCode(200))

        val result = repository.getTrendingNews()

        assertThat(result).isInstanceOf(NewsResult.Error::class.java)
    }

    companion object {
        private val SAMPLE_RESPONSE_JSON = """
            {
              "count": 1,
              "items": [
                {
                  "id": "item-1",
                  "headline": "Egypt central bank holds interest rates",
                  "summary": "The CBE kept rates unchanged.",
                  "source": "Reuters",
                  "source_url": "https://example.com/cbe-rates",
                  "published_at": "2026-09-01T09:00:00+00:00",
                  "fetched_at": "2026-09-01T09:05:00+00:00",
                  "data_status": "DELAYED",
                  "tickers": [
                    {
                      "symbol": "COMI",
                      "company_name": "Commercial International Bank (Egypt)",
                      "market": "egx",
                      "sentiment_score": 0.1,
                      "sentiment_label": "neutral"
                    }
                  ],
                  "verification_status": "SINGLE_SOURCE",
                  "corroborating_sources": ["Reuters"],
                  "potential_impact": ["EGX-listed importers/exporters"]
                }
              ]
            }
        """.trimIndent()

        private val EMPTY_RESPONSE_JSON = """{"count": 0, "items": []}"""
    }
}
