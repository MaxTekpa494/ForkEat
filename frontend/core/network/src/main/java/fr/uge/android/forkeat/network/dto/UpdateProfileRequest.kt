package fr.uge.android.forkeat.network.dto

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val username: String
)