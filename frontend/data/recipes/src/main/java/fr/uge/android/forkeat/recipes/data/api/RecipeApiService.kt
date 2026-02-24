package fr.uge.android.forkeat.recipes.data.api

import fr.uge.android.forkeat.recipes.data.dto.CreateRecipeFormDataResponse
import fr.uge.android.forkeat.recipes.data.dto.RecipeItemResponse
import fr.uge.android.forkeat.recipes.data.dto.RecipesListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

interface RecipeApiService {

    @GET("api/recipes")
    suspend fun getRecipes(
        @Query("status") status: String = "PUBLISHED",
        @Query("size") size: Int = 10,
        @Query("page") page: Int = 0,
        @Query("search") search: String? = null,
        @Query("allergens") allergens: List<String>? = null
    ): Response<RecipesListResponse>

    @GET("api/recipes/{id}")
    suspend fun getRecipe(@Path("id") id: UUID): Response<RecipeItemResponse>

    @GET("api/recipes/create")
    suspend fun getCreateRecipeData(): Response<CreateRecipeFormDataResponse>

    @Multipart
    @POST("api/recipes/create")
    suspend fun createRecipe(
        @Part("recipe") recipe: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<RecipeItemResponse>

    @Multipart
    @POST("api/recipes/{id}/update")
    suspend fun updateRecipe(
        @Path("id") id: UUID,
        @Part("recipe") recipe: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<RecipeItemResponse>

    @GET("api/recipes/my-recipes")
    suspend fun getMyRecipes(): Response<RecipesListResponse>

    @POST("api/recipes/{id}/delete")
    suspend fun deleteRecipe(@Path("id") id: UUID): Response<Void>
}
