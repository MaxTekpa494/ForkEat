package fr.uge.android.forkeat.profile.data.api

import fr.uge.android.forkeat.profile.data.dto.UpdateProfileRequest
import fr.uge.android.forkeat.network.dto.UserDTO
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.profile.data.dto.PasswordChangeRequest
import fr.uge.android.forkeat.profile.data.dto.RequestEmailChangeRequest
import fr.uge.android.forkeat.profile.data.dto.SetPasswordRequest
import retrofit2.Response
import retrofit2.http.*

object AccountApi {
    val service: AccountApiService by lazy {
        ForkEatApi.createService(AccountApiService::class.java)
    }
}

interface AccountApiService {
    @GET("api/account")
    suspend fun getAccount(): Response<UserDTO>

    @PUT("api/account")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDTO>

    @PUT("api/account/password")
    suspend fun setPassword(@Body request: SetPasswordRequest): Response<Unit>

    @POST("api/account/password-change-requests")
    suspend fun requestPasswordChange(@Body request: PasswordChangeRequest): Response<Unit>

    @PUT("api/account/password-change-requests")
    suspend fun confirmPasswordChange(@Query("code") code: String): Response<Unit>

    @POST("api/account/email-change-requests")
    suspend fun requestEmailChange(@Body request: RequestEmailChangeRequest): Response<Unit>

    @PUT("api/account/email-change-requests")
    suspend fun confirmEmailChange(@Query("code") code: String): Response<UserDTO>

    @POST("api/account/email-confirmations")
    suspend fun resendConfirmation(): Response<Unit>

    @POST("api/account/delete-requests")
    suspend fun requestAccountDeletion(): Response<Unit>

    @PUT("api/account/delete-requests")
    suspend fun confirmAccountDeletion(@Query("code") code: String): Response<Unit>
}