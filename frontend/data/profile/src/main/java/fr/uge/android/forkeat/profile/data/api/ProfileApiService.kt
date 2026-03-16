package fr.uge.android.forkeat.profile.data.api

import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.profile.data.dto.UserDashboardDTO
import fr.uge.android.forkeat.profile.data.dto.UserProfileResponseDTO
import fr.uge.android.forkeat.profile.data.dto.UserReportRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

object ProfileApi {
    val service: ProfileApiService by lazy {
        ForkEatApi.createService(ProfileApiService::class.java)
    }
}

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

    @POST("api/profile/{username}/reports")
    suspend fun reportUser(
        @Header("Authorization") token: String,
        @Path("username") username: String,
        @Body request: UserReportRequestDTO
    ): Response<Unit>
}
