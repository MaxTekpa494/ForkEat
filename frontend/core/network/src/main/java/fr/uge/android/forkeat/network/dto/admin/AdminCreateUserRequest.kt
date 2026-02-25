package fr.uge.android.forkeat.network.dto.admin

data class AdminCreateUserRequest(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)
