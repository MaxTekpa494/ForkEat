package fr.uge.android.forkeat.moderator.data.api

import fr.uge.android.forkeat.moderator.data.dto.RecipeReportDetails
import fr.uge.android.forkeat.moderator.data.dto.ReportListResponse
import fr.uge.android.forkeat.moderator.data.dto.UserModerationRequest
import fr.uge.android.forkeat.moderator.data.dto.UserReportDetails
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.dto.SimpleRecipesListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

object ModeratorApi {
    val service: ModeratorApiService by lazy {
        ForkEatApi.createService(ModeratorApiService::class.java)
    }
}

data class RejectRecipeRequest(val justification: String)

data class ValidateReportRequest(val recipeId: UUID)

data class DismissReportRequest(val recipeId: UUID, val justification: String)

interface ModeratorApiService {

    @GET("api/moderator/recipes/pending")
    suspend fun getPendingRecipes(
      @Query("page") page: Int = 0,
      @Query("size") size: Int = 10,
    ): Response<SimpleRecipesListResponse>

    @POST("api/moderator/recipes/{id}/validate")
    suspend fun validateRecipe(
      @Path("id") id: String,
    ): Response<Unit>

    @POST("api/moderator/recipes/{id}/reject")
    suspend fun rejectRecipe(
      @Path("id") id: String,
      @Body request: RejectRecipeRequest
    ): Response<Unit>

    @GET("api/moderator/recipes/reports")
    suspend fun getReportedRecipes(
      @Query("size") size: Int,
      @Query("page") page: Int
    ): Response<ReportListResponse<RecipeReportDetails>>

    @GET("api/moderator/users/reports")
    suspend fun getReportedUsers(
      @Query("size") size: Int,
      @Query("page") page: Int
    ): Response<ReportListResponse<UserReportDetails>>

    @POST("api/moderator/recipes/reports/{reportId}/validate")
    suspend fun validateReport(
      @Path("reportId") reportId: UUID,
      @Body request: ValidateReportRequest
    ): Response<Unit>

    @POST("api/moderator/recipes/reports/{reportId}/dismiss")
    suspend fun dismissReport(
      @Path("reportId") reportId: UUID,
      @Body request: DismissReportRequest
    ): Response<Unit>

    @POST("api/moderator/users/reports/{reportId}/resolve")
    suspend fun resolveUserReport(
      @Path("reportId") reportId: UUID,
      @Body request: UserModerationRequest
    ): Response<Unit>
}
