package fr.uge.android.forkeat.network.api

import fr.uge.android.forkeat.network.dto.UserDashboardDTO
import fr.uge.android.forkeat.network.dto.UserProfileResponseDTO
import retrofit2.Response
import retrofit2.http.GET
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
}
