package com.globalpulse.news.domain.repository

import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.domain.model.NewsItem

/**
 * Explicit success/failure modeling instead of throwing — callers (the
 * ViewModel) are forced to handle both cases rather than relying on a
 * try/catch they might forget.
 */
sealed interface NewsResult {
    data class Success(val items: List<NewsItem>) : NewsResult
    data class Error(val message: String) : NewsResult
}

interface NewsRepository {
    suspend fun getTrendingNews(market: Market? = null, includeGeneral: Boolean = true): NewsResult
}
