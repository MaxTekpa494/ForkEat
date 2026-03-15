package fr.uge.android.forkeat.profile.data.dto

data class SetPasswordRequest(
    val newPassword: String,
    val confirmPassword: String
)
