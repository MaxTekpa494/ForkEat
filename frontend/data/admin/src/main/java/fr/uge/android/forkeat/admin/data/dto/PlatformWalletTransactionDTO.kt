package fr.uge.android.forkeat.admin.data.dto

data class PlatformWalletTransactionDTO(
    val id: String,
    val walletType: String,
    val amountCents: Long,
    val reason: String,
    val referenceId: String?,
    val createdAt: String
)