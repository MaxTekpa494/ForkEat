package fr.uge.android.forkeat.network.dto

data class ConfirmPasswordChangeRequest(
    val code: String,
    val newPassword: String,
    val confirmPassword: String
)
