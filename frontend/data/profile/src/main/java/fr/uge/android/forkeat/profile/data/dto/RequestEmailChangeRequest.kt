package fr.uge.android.forkeat.profile.data.dto

data class RequestEmailChangeRequest(
    val newEmail: String,
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)