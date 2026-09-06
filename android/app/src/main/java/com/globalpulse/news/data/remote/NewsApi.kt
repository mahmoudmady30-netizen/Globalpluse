package com.globalpulse.news.data.remote

import com.globalpulse.news.data.remote.dto.NewsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {
    @GET("api/news")
    suspend fun getNews(
        @Query("market") market: String? = null,
        @Query("include_general") includeGeneral: Boolean = true
    ): NewsResponseDto
}
