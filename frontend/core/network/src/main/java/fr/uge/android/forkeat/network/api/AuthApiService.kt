package fr.uge.android.forkeat.network.api

import fr.uge.android.forkeat.network.dto.ForgottenPasswordCodeRequest
import fr.uge.android.forkeat.network.dto.ForgottenPasswordRequest
import fr.uge.android.forkeat.network.dto.GoogleLoginRequest
import fr.uge.android.forkeat.network.dto.LoginRequest
import fr.uge.android.forkeat.network.dto.LoginApiResponse
import fr.uge.android.forkeat.network.dto.RegisterRequest
import fr.uge.android.forkeat.network.dto.UserDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginApiResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("api/auth/forgot-password")
    suspend fun askForgottenPassword(@Body request: ForgottenPasswordRequest): Response<Unit>

    @POST("api/auth/forgot-password/confirm-code")
     suspend fun sendForgottenPasswordCode(@Body request: ForgottenPasswordCodeRequest): Response<Unit>

    @POST("api/auth/google-login")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<LoginApiResponse>

    @GET("api/auth/me")
    suspend fun me(): Response<UserDTO>

}
