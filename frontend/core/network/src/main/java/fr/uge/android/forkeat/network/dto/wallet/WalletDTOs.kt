package fr.uge.android.forkeat.network.dto.wallet

data class BalanceResponse(
    val balance: Long
)

data class TransactionDTO(
    val id: String,
    val walletSourceId: String?,
    val walletDestinationId: String?,
    val amount: Long,
    val type: String,
    val createdAt: String,
    val stripeTransactionID: String?,
    val status: String?
)

data class TopUpRequest(
    val amount: Long,
    val source: String = "android"
)

data class RechargeResponse(
    val url: String
)

data class CreateBankInfoRequest(
    val bankName: String,
    val iban: String,
    val bic: String
)

data class BankInfoResponse(
    val userId: String,
    val bankName: String,
    val externalAccountId: String
)

data class WithdrawRequest(
    val amount: Long
)

data class WithdrawResponse(
    val message: String,
    val payoutId: String
)
