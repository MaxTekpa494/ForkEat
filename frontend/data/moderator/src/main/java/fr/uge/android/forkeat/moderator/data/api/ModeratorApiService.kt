package fr.uge.android.forkeat.moderator.data.api

import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.dto.RecipesListResponse
import fr.uge.android.forkeat.recipes.data.dto.SimpleRecipesListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

object ModeratorApi {
    val service: ModeratorApiService by lazy {
        ForkEatApi.createService(ModeratorApiService::class.java)
    }
}

data class RejectRecipeRequest(val justification: String)

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
}
