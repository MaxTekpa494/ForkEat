package fr.uge.android.forkeat.network.api

import fr.uge.android.forkeat.network.dto.ConfirmPasswordChangeRequest
import fr.uge.android.forkeat.network.dto.PasswordChangeRequest
import fr.uge.android.forkeat.network.dto.SetPasswordRequest
import fr.uge.android.forkeat.network.dto.UserDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AccountApiService {
    @GET("api/account")
    suspend fun getAccount(): Response<UserDTO>

    @POST("api/account/set-password")
    suspend fun setPassword(@Body request: SetPasswordRequest): Response<Unit>

    @POST("api/account/request-password-change")
    suspend fun requestPasswordChange(@Body request: PasswordChangeRequest): Response<Unit>

    @POST("api/account/confirm-password-change")
    suspend fun confirmPasswordChange(@Body request: ConfirmPasswordChangeRequest): Response<Unit>
}
