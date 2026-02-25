package fr.uge.android.forkeat.network.api

import fr.uge.android.forkeat.network.dto.UserDTO
import fr.uge.android.forkeat.network.dto.admin.AdminCreateUserRequest
import fr.uge.android.forkeat.network.dto.admin.AdminRecipeStatsDTO
import fr.uge.android.forkeat.network.dto.admin.AdminRecipesListResponse
import fr.uge.android.forkeat.network.dto.admin.AdminUserStatsDTO
import fr.uge.android.forkeat.network.dto.admin.AdminUsersListResponse
import fr.uge.android.forkeat.network.dto.admin.PlatformWalletDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApiService {

    @GET("api/admin/stats/users")
    suspend fun getUserStats(): Response<AdminUserStatsDTO>

    @GET("api/admin/stats/recipes")
    suspend fun getRecipeStats(): Response<AdminRecipeStatsDTO>

    @GET("api/admin/wallets/benefits")
    suspend fun getBenefitsWallet(): Response<PlatformWalletDTO>

    @GET("api/admin/wallets/redistribution")
    suspend fun getRedistributionWallet(): Response<PlatformWalletDTO>

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

    @GET("api/admin/recipes/pending")
    suspend fun getPendingRecipes(): Response<AdminRecipesListResponse>

    @GET("api/admin/recipes/published")
    suspend fun getPublishedRecipes(@Query("page") page: Int = 0): Response<AdminRecipesListResponse>

    @POST("api/admin/recipes/{id}/validate")
    suspend fun validateRecipe(@Path("id") id: String): Response<Unit>

    @POST("api/admin/recipes/{id}/reject")
    suspend fun rejectRecipe(@Path("id") id: String): Response<Unit>
}
