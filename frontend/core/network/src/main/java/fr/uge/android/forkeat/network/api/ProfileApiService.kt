package fr.uge.android.forkeat.network.api

import fr.uge.android.forkeat.network.dto.UserDashboardDTO
import fr.uge.android.forkeat.network.dto.UserProfileResponseDTO
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApiService {
    @GET("api/profile")
    suspend fun getMyProfile(): Response<UserDashboardDTO>

    @GET("api/profile/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 12
    ): Response<UserProfileResponseDTO>

    @PUT("api/profile/{username}/follow")
    suspend fun followUser(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): Response<Unit>

    @DELETE("api/profile/{username}/follow")
    suspend fun unfollowUser(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): Response<Unit>
}
