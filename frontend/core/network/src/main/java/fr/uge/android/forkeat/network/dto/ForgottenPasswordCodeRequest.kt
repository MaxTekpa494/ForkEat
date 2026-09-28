package fr.uge.android.forkeat.network.dto

data class ForgottenPasswordCodeRequest(val email: String, val code: String, val password: String, val confirmPassword: String)
