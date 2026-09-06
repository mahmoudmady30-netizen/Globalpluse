package com.globalpulse.news.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.domain.repository.NewsRepository
import com.globalpulse.news.domain.repository.NewsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        loadNews()
    }

    fun onMarketSelected(market: Market?) {
        if (market == _uiState.value.selectedMarket) return
        _uiState.update { it.copy(selectedMarket = market) }
        loadNews()
    }

    fun refresh() {
        loadNews()
    }

    private fun loadNews() {
        val market = _uiState.value.selectedMarket
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.getTrendingNews(market = market)) {
                is NewsResult.Success -> _uiState.update {
                    it.copy(isLoading = false, items = result.items, errorMessage = null)
                }
                is NewsResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}

