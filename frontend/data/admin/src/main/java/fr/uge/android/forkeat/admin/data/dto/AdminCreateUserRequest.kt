package fr.uge.android.forkeat.admin.data.dto

data class AdminCreateUserRequest(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)
