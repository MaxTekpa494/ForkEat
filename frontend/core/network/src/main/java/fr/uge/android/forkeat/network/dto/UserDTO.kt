package fr.uge.android.forkeat.network.dto

data class UserDTO(
    val resource: UserResource
)

data class UserResource(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val status: String,
    val authMode: String,
    val emailVerified: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)
