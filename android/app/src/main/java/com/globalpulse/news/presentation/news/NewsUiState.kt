package com.globalpulse.news.presentation.news

import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.domain.model.NewsItem

data class NewsUiState(
    val isLoading: Boolean = true,
    val items: List<NewsItem> = emptyList(),
    val selectedMarket: Market? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && items.isEmpty()
}
