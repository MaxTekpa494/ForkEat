package fr.uge.android.forkeat.network.api

import fr.uge.android.forkeat.network.dto.ForgottenPasswordCodeRequest
import fr.uge.android.forkeat.network.dto.ForgottenPasswordRequest
import fr.uge.android.forkeat.network.dto.LoginRequest
import fr.uge.android.forkeat.network.dto.LoginResponse
import fr.uge.android.forkeat.network.dto.NewPasswordRequest
import fr.uge.android.forkeat.network.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("api/auth/forgot-password")
    suspend fun askForgottenPassword(@Body request: ForgottenPasswordRequest): Response<Unit>

    @POST("api/auth/forgot-password/confirm-code")
     suspend fun sendForgottenPasswordCode(@Body request: ForgottenPasswordCodeRequest): Response<Unit>

}
