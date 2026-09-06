package com.globalpulse.news.data.repository

import com.globalpulse.news.data.remote.NewsApi
import com.globalpulse.news.domain.model.Market
import com.globalpulse.news.domain.repository.NewsRepository
import com.globalpulse.news.domain.repository.NewsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class NewsRepositoryImpl(
    private val api: NewsApi,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : NewsRepository {

    override suspend fun getTrendingNews(market: Market?, includeGeneral: Boolean): NewsResult =
        withContext(ioDispatcher) {
            try {
                val response = api.getNews(
                    market = market?.name?.lowercase(),
                    includeGeneral = includeGeneral
                )
                val domainItems = response.items.mapNotNull { it.toDomain() }
                NewsResult.Success(domainItems)
            } catch (e: HttpException) {
                NewsResult.Error(httpErrorMessage(e))
            } catch (e: IOException) {
                NewsResult.Error("تعذّر الاتصال بالخادم. تأكد من اتصال الإنترنت وأن رابط الخادم صحيح.")
            } catch (e: SerializationException) {
                // The backend returned a 2xx response, but the body wasn't the
                // JSON shape we expect (malformed, or an unexpected schema
                // change). Surfacing this as a normal error rather than
                // letting it crash the app is the whole point of this catch —
                // this test failure is exactly what caught the original gap.
                NewsResult.Error("رد غير متوقع من الخادم. جرّب تاني بعد شوية.")
            }
        }

    private fun httpErrorMessage(e: HttpException): String = when (e.code()) {
        503 -> "الخادم غير متاح مؤقتًا (قد تكون الكوتة اليومية للمصدر انتهت)."
        401, 403 -> "خطأ في الصلاحيات — تأكد من مفاتيح الـ API على الخادم."
        else -> "خطأ من الخادم (${e.code()})."
    }
}
