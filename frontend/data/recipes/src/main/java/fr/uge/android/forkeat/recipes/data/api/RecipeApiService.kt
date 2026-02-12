package fr.uge.android.forkeat.recipes.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import fr.uge.android.forkeat.recipes.data.dto.RecipesListResponse

interface RecipeApiService {
    @GET("api/recipes")
    suspend fun getRecipes(
        @Query("status") status: String = "PUBLISHED",
        @Query("size") size: Int = 10,
        @Query("page") page: Int = 0,
        @Query("search") search: String? = null,
        @Query("allergens") allergens: List<String>? = null
    ): Response<RecipesListResponse>
}