package fr.uge.android.forkeat.network.dto

data class LoginResponse(
    val token: String,
    val type: String,
)

data class LoginApiResponse(
    val resource: LoginResponse
)
