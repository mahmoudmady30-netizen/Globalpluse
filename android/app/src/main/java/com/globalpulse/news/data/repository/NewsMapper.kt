package com.globalpulse.news.data.repository

import com.globalpulse.news.data.remote.dto.NewsItemDto
import com.globalpulse.news.data.remote.dto.TickerMentionDto
import com.globalpulse.news.domain.model.DataStatus
import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.domain.model.NewsItem
import com.globalpulse.news.domain.model.SentimentLabel
import com.globalpulse.news.domain.model.TickerMention
import com.globalpulse.news.domain.model.VerificationStatus
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Maps a raw API DTO into a domain model. Unknown/unexpected enum strings
 * from the backend fall back to a safe default rather than crashing the
 * whole feed over one malformed field — see [safeEnum].
 */
fun NewsItemDto.toDomain(): NewsItem? {
    val parsedDate = parsePublishedAt(publishedAt) ?: return null

    return NewsItem(
        id = id,
        headline = headline,
        summary = summary,
        source = source,
        sourceUrl = sourceUrl,
        publishedAt = parsedDate,
        dataStatus = safeEnum(dataStatus, DataStatus.DELAYED),
        tickers = tickers.mapNotNull { it.toDomain() },
        verificationStatus = safeEnum(verificationStatus, VerificationStatus.SINGLE_SOURCE),
        corroboratingSources = corroboratingSources,
        potentialImpact = potentialImpact
    )
}

fun TickerMentionDto.toDomain(): TickerMention? {
    val marketEnum = safeEnumOrNull<Market>(market) ?: return null
    return TickerMention(
        symbol = symbol,
        companyName = companyName,
        market = marketEnum,
        sentimentScore = sentimentScore,
        sentimentLabel = safeEnum(sentimentLabel, SentimentLabel.NEUTRAL)
    )
}

private fun parsePublishedAt(raw: String): OffsetDateTime? = try {
    OffsetDateTime.parse(raw)
} catch (e: DateTimeParseException) {
    null
}

private inline fun <reified T : Enum<T>> safeEnum(raw: String, default: T): T =
    safeEnumOrNull<T>(raw) ?: default

private inline fun <reified T : Enum<T>> safeEnumOrNull(raw: String): T? =
    enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) }
