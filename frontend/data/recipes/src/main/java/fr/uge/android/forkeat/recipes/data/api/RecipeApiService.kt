package fr.uge.android.forkeat.recipes.data.api

import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.dto.RecipeAllergenDTO
import fr.uge.android.forkeat.recipes.data.dto.CreateRecipeFormDataResponse
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import fr.uge.android.forkeat.recipes.data.dto.AuthorRecipesResponse
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDetailsResponseDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeItemResponse
import fr.uge.android.forkeat.recipes.data.dto.RecipeReportRequestDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipesListResponse
import fr.uge.android.forkeat.recipes.data.dto.SmartSearchConfigResponse
import fr.uge.android.forkeat.recipes.data.dto.SmartSearchRequestDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

object RecipeApi {
    val service: RecipeApiService by lazy {
        ForkEatApi.createService(RecipeApiService::class.java)
    }
}

interface RecipeApiService {

    @GET("api/recipes")
    suspend fun getRecipes(
        @Header("Authorization") token: String?,
        @Query("status") status: String = "PUBLISHED",
        @Query("size") size: Int = 10,
        @Query("page") page: Int = 0,
        @Query("search") search: String? = null,
        @Query("allergens") allergens: List<String>? = null
    ): Response<RecipesListResponse<PersonalizedRecipeSummaryDTO>>

    @GET("api/recipes/{id}")
    suspend fun getRecipeWithId(
        @Header("Authorization") token: String?,
        @Path("id") id: UUID
    ): Response<RecipeDetailsResponseDTO>

    @GET("api/recipes/create")
    suspend fun getCreateRecipeData(): Response<CreateRecipeFormDataResponse>

    @Multipart
    @POST("api/recipes/create")
    suspend fun createRecipe(
        @Part("recipe") recipe: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<RecipeItemResponse>

    @Multipart
    @POST("api/recipes/create-variant")
    suspend fun createVariant(
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
    suspend fun getMyRecipes(
        @Query("status") status: String = "PUBLISHED",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<AuthorRecipesResponse>

    @POST("api/recipes/{id}/delete")
    suspend fun deleteRecipe(@Path("id") id: UUID): Response<Void>

    @POST("api/recipes/{id}/like")
    suspend fun likeRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: UUID
    ): Response<Unit>

    @POST("api/recipes/{id}/super-like")
    suspend fun superLikeRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: UUID
    ): Response<Unit>

    @DELETE("api/recipes/{id}/like")
    suspend fun unlikeRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: UUID
    ): Response<Unit>

    @PUT("api/recipes/{id}/follow")
    suspend fun followRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: UUID
    ): Response<Unit>

    @DELETE("api/recipes/{id}/follow")
    suspend fun unfollowRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: UUID
    ): Response<Unit>

    @GET("api/recipes/allergens")
    suspend fun getAllergens(): Response<RecipesListResponse<RecipeAllergenDTO>>

    @POST("api/recipes/{id}/reports")
    suspend fun reportRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: UUID,
        @Body request: RecipeReportRequestDTO
    ): Response<Unit>

    @GET("api/recipes/smart-search/config")
    suspend fun getSmartSearchConfig(): Response<SmartSearchConfigResponse>

    @POST("api/recipes/smart-search")
    suspend fun smartSearch(
        @Body request: SmartSearchRequestDTO
    ): Response<RecipesListResponse<PersonalizedRecipeSummaryDTO>>
}
