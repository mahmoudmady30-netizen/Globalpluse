package com.globalpulse.news.data.di

import com.globalpulse.news.BuildConfig
import com.globalpulse.news.data.remote.NewsApi
import com.globalpulse.news.data.repository.NewsRepositoryImpl
import com.globalpulse.news.domain.repository.NewsRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual dependency provisioning. Deliberately not Hilt/Dagger: for a
 * single-screen app, a DI framework adds build-time and cognitive overhead
 * without a corresponding benefit. If the app grows past a couple of
 * screens/repositories, migrating to Hilt here is a contained change.
 */
object ServiceLocator {

    private val json = Json { ignoreUnknownKeys = true }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val newsApi: NewsApi by lazy { retrofit.create(NewsApi::class.java) }

    val newsRepository: NewsRepository by lazy { NewsRepositoryImpl(newsApi) }
}
