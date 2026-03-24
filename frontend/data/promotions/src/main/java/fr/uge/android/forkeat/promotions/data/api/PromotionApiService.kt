package fr.uge.android.forkeat.promotions.data.api

import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.promotions.data.dto.PromotionDTO
import fr.uge.android.forkeat.promotions.data.dto.PromotionItemResponse
import fr.uge.android.forkeat.promotions.data.dto.PromotionListResponse
import retrofit2.Response
import retrofit2.http.GET

object PromotionApi {
    val service: PromotionApiService by lazy {
        ForkEatApi.createService(PromotionApiService::class.java)
    }
}

interface PromotionApiService {

    @GET("api/promotions/active")
    suspend fun getActivePromotion(): Response<PromotionItemResponse>

    @GET("api/promotions/upcoming")
    suspend fun getUpcomingPromotions(): Response<PromotionListResponse<PromotionDTO>>
}
