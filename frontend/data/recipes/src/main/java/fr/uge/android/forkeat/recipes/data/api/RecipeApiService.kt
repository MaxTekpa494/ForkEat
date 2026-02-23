package fr.uge.android.forkeat.recipes.data.api

import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDetailsDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDetailsResponseDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Header
import java.util.UUID
import retrofit2.http.Query
import fr.uge.android.forkeat.recipes.data.dto.RecipesListResponse

interface RecipeApiService {
    @GET("api/recipes")
    suspend fun getRecipes(
        @Header("Authorization") token: String,
        @Query("status") status: String = "PUBLISHED",
        @Query("size") size: Int = 10,
        @Query("page") page: Int = 0,
        @Query("search") search: String? = null,
        @Query("allergens") allergens: List<String>? = null
    ): Response<RecipesListResponse>

    @GET("api/recipes/{id}")
    suspend fun getRecipeWithId(
        @Header("Authorization") token: String,
        @Path("id") id: UUID
    ): Response<RecipeDetailsResponseDTO>

    @POST("api/recipes/{id}/like")
    suspend fun likeRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: UUID
    ): Response<Unit>

    @DELETE("api/recipes/{id}/like")
    suspend fun unlikeRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: UUID
    ): Response<Unit>
}