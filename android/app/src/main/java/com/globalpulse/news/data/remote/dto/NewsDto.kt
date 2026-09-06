package com.globalpulse.news.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TickerMentionDto(
    val symbol: String,
    @SerialName("company_name") val companyName: String,
    val market: String,
    @SerialName("sentiment_score") val sentimentScore: Float,
    @SerialName("sentiment_label") val sentimentLabel: String
)

@Serializable
data class NewsItemDto(
    val id: String,
    val headline: String,
    val summary: String? = null,
    val source: String,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("fetched_at") val fetchedAt: String,
    @SerialName("data_status") val dataStatus: String,
    val tickers: List<TickerMentionDto> = emptyList(),
    @SerialName("verification_status") val verificationStatus: String = "SINGLE_SOURCE",
    @SerialName("corroborating_sources") val corroboratingSources: List<String> = emptyList(),
    @SerialName("potential_impact") val potentialImpact: List<String> = emptyList()
)

@Serializable
data class NewsResponseDto(
    val count: Int,
    val items: List<NewsItemDto>
)
