package fr.uge.android.forkeat.profile.data.dto

data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)
