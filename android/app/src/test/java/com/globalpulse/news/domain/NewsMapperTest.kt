package com.globalpulse.news.domain

import com.globalpulse.news.data.remote.dto.NewsItemDto
import com.globalpulse.news.data.remote.dto.TickerMentionDto
import com.globalpulse.news.data.repository.toDomain
import com.globalpulse.news.domain.model.DataStatus
import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.domain.model.SentimentLabel
import com.globalpulse.news.domain.model.VerificationStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NewsMapperTest {

    private fun sampleDto(
        publishedAt: String = "2026-09-01T10:00:00+00:00",
        tickers: List<TickerMentionDto> = listOf(
            TickerMentionDto(
                symbol = "AAPL",
                companyName = "Apple Inc.",
                market = "us",
                sentimentScore = 0.4f,
                sentimentLabel = "bullish"
            )
        ),
        dataStatus: String = "DELAYED",
        verificationStatus: String = "MULTI_SOURCE_VERIFIED"
    ) = NewsItemDto(
        id = "abc-123",
        headline = "Apple reports strong Q3 earnings",
        summary = "Apple beat expectations.",
        source = "Reuters",
        sourceUrl = "https://example.com/apple-earnings",
        publishedAt = publishedAt,
        fetchedAt = "2026-09-01T10:05:00+00:00",
        dataStatus = dataStatus,
        tickers = tickers,
        verificationStatus = verificationStatus,
        corroboratingSources = listOf("Reuters", "Bloomberg"),
        potentialImpact = listOf("Tech sector sentiment")
    )

    @Test
    fun `maps a well-formed DTO into a domain NewsItem`() {
        val domain = sampleDto().toDomain()

        assertThat(domain).isNotNull()
        assertThat(domain!!.id).isEqualTo("abc-123")
        assertThat(domain.headline).isEqualTo("Apple reports strong Q3 earnings")
        assertThat(domain.dataStatus).isEqualTo(DataStatus.DELAYED)
        assertThat(domain.verificationStatus).isEqualTo(VerificationStatus.MULTI_SOURCE_VERIFIED)
        assertThat(domain.corroboratingSources).containsExactly("Reuters", "Bloomberg")
        assertThat(domain.potentialImpact).containsExactly("Tech sector sentiment")
    }

    @Test
    fun `maps ticker mentions with correct market and sentiment`() {
        val domain = sampleDto().toDomain()!!

        assertThat(domain.tickers).hasSize(1)
        val ticker = domain.tickers.first()
        assertThat(ticker.symbol).isEqualTo("AAPL")
        assertThat(ticker.market).isEqualTo(Market.US)
        assertThat(ticker.sentimentLabel).isEqualTo(SentimentLabel.BULLISH)
    }

    @Test
    fun `primaryTicker returns the first ticker`() {
        val domain = sampleDto().toDomain()!!
        assertThat(domain.primaryTicker?.symbol).isEqualTo("AAPL")
    }

    @Test
    fun `returns null when published_at cannot be parsed`() {
        val domain = sampleDto(publishedAt = "not-a-date").toDomain()
        assertThat(domain).isNull()
    }

    @Test
    fun `falls back to a safe default for an unknown data_status instead of crashing`() {
        val domain = sampleDto(dataStatus = "SOMETHING_NEW_FROM_BACKEND").toDomain()!!
        assertThat(domain.dataStatus).isEqualTo(DataStatus.DELAYED)
    }

    @Test
    fun `ticker with an unrecognized market is dropped, not crashed on`() {
        val dto = sampleDto(
            tickers = listOf(
                TickerMentionDto(
                    symbol = "XYZ",
                    companyName = "Unknown Co",
                    market = "gcc", // not a market this app supports yet
                    sentimentScore = 0f,
                    sentimentLabel = "neutral"
                )
            )
        )
        val domain = dto.toDomain()!!
        assertThat(domain.tickers).isEmpty()
    }

    @Test
    fun `item with no tickers still maps (general world news)`() {
        val dto = sampleDto(tickers = emptyList())
        val domain = dto.toDomain()!!
        assertThat(domain.tickers).isEmpty()
        assertThat(domain.primaryTicker).isNull()
    }
}
