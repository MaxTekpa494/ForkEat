package fr.uge.android.forkeat.network.dto

data class RequestEmailChangeRequest(
    val newEmail: String,
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)