package com.globalpulse.news.domain.model

import java.time.OffsetDateTime

enum class Market { US, EGX }

enum class SentimentLabel { BULLISH, NEUTRAL, BEARISH }

enum class DataStatus { LIVE, DELAYED, HISTORICAL, CACHED }

enum class VerificationStatus { SINGLE_SOURCE, MULTI_SOURCE_VERIFIED, CONFLICTING_REPORTS }

data class TickerMention(
    val symbol: String,
    val companyName: String,
    val market: Market,
    val sentimentScore: Float,
    val sentimentLabel: SentimentLabel
)

data class NewsItem(
    val id: String,
    val headline: String,
    val summary: String?,
    val source: String,
    val sourceUrl: String,
    val publishedAt: OffsetDateTime,
    val dataStatus: DataStatus,
    val tickers: List<TickerMention>,
    val verificationStatus: VerificationStatus,
    val corroboratingSources: List<String>,
    val potentialImpact: List<String>
) {
    /** Convenience for the UI — the first ticker mentioned, if any, drives the row's accent color. */
    val primaryTicker: TickerMention?
        get() = tickers.firstOrNull()
}
