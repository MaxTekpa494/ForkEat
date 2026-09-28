package fr.uge.android.forkeat.network.dto

data class ForgotPasswordStatusDTO(
    val resource: Resource
) {
    data class Resource(
        val requiresCode: Boolean
    )
}
