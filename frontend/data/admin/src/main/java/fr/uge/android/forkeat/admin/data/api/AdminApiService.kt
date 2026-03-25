package fr.uge.android.forkeat.admin.data.api

import fr.uge.android.forkeat.admin.data.dto.AdminCreateUserRequest
import fr.uge.android.forkeat.admin.data.dto.AdminItemResponse
import fr.uge.android.forkeat.admin.data.dto.AdminRecipeStatsDTO
import fr.uge.android.forkeat.admin.data.dto.AdminUserStatsDTO
import fr.uge.android.forkeat.admin.data.dto.AdminUsersListResponse
import fr.uge.android.forkeat.admin.data.dto.CreatePromotionRequest
import fr.uge.android.forkeat.admin.data.dto.PlatformWalletDTO
import fr.uge.android.forkeat.admin.data.dto.SuperLikeConfigDTO
import fr.uge.android.forkeat.admin.data.dto.UpdatePromotionRequest
import fr.uge.android.forkeat.admin.data.dto.UpdateSuperLikeConfigRequest
import fr.uge.android.forkeat.admin.data.dto.PlatformWalletTransactionDTO
import fr.uge.android.forkeat.network.dto.UserDTO
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.promotions.data.dto.PromotionDTO
import fr.uge.android.forkeat.promotions.data.dto.PromotionItemResponse
import fr.uge.android.forkeat.promotions.data.dto.PromotionListResponse
import fr.uge.android.forkeat.recipes.data.dto.SimpleRecipesListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

object AdminApi {
    val service: AdminApiService by lazy {
        ForkEatApi.createService(AdminApiService::class.java)
    }
}

interface AdminApiService {

    @GET("api/admin/stats/users")
    suspend fun getUserStats(): Response<AdminUserStatsDTO>

    @GET("api/admin/stats/recipes")
    suspend fun getRecipeStats(): Response<AdminRecipeStatsDTO>

    @GET("api/admin/wallets/benefits")
    suspend fun getBenefitsWallet(): Response<PlatformWalletDTO>

    @GET("api/admin/wallets/redistribution")
    suspend fun getRedistributionWallet(): Response<PlatformWalletDTO>

    @GET("api/admin/wallets/transactions")
    suspend fun getWalletTransactions(): Response<List<PlatformWalletTransactionDTO>>

    @GET("api/admin/admins")
    suspend fun getAdmins(): Response<AdminUsersListResponse>

    @GET("api/admin/users")
    suspend fun getMembers(): Response<AdminUsersListResponse>

    @GET("api/admin/moderators")
    suspend fun getModerators(): Response<AdminUsersListResponse>

    @POST("api/admin/register")
    suspend fun createModerator(@Body request: AdminCreateUserRequest): Response<UserDTO>

    @POST("api/admin/admins")
    suspend fun createAdmin(@Body request: AdminCreateUserRequest): Response<UserDTO>

    @GET("api/admin/recipes/published")
    suspend fun getPublishedRecipes(@Query("page") page: Int = 0): Response<SimpleRecipesListResponse>

    // ── Promotions ────────────────────────────────────────────────────────────

    @GET("api/admin/promotions")
    suspend fun getAllPromotions(): Response<PromotionListResponse<PromotionDTO>>

    @GET("api/admin/promotions/{id}")
    suspend fun getPromotion(@Path("id") id: String): Response<PromotionItemResponse>

    @POST("api/admin/promotions")
    suspend fun createPromotion(@Body request: CreatePromotionRequest): Response<PromotionItemResponse>

    @PUT("api/admin/promotions/{id}")
    suspend fun updatePromotion(
        @Path("id") id: String,
        @Body request: UpdatePromotionRequest
    ): Response<PromotionItemResponse>

    @DELETE("api/admin/promotions/{id}")
    suspend fun cancelPromotion(@Path("id") id: String): Response<Void>

    // ── Redistribution ───────────────────────────────────────────────────────

    @POST("api/admin/redistribution/trigger")
    suspend fun triggerRedistribution(): Response<Void>

    // ── Super-Like Config ─────────────────────────────────────────────────────

    @GET("api/admin/super-like/config")
    suspend fun getSuperLikeConfig(): Response<AdminItemResponse<SuperLikeConfigDTO>>

    @PUT("api/admin/super-like/config")
    suspend fun updateSuperLikeConfig(@Body request: UpdateSuperLikeConfigRequest): Response<AdminItemResponse<SuperLikeConfigDTO>>
}
