package fr.uge.android.forkeat.network.api

import fr.uge.android.forkeat.network.dto.wallet.BalanceResponse
import fr.uge.android.forkeat.network.dto.wallet.BankInfoResponse
import fr.uge.android.forkeat.network.dto.wallet.CreateBankInfoRequest
import fr.uge.android.forkeat.network.dto.wallet.RechargeResponse
import fr.uge.android.forkeat.network.dto.wallet.TopUpRequest
import fr.uge.android.forkeat.network.dto.wallet.TransactionDTO
import fr.uge.android.forkeat.network.dto.wallet.WithdrawRequest
import fr.uge.android.forkeat.network.dto.wallet.WithdrawResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WalletApiService {

    @GET("api/wallet/balance")
    suspend fun getBalance(): Response<BalanceResponse>

    @GET("api/wallet/transactions")
    suspend fun getTransactions(): Response<List<TransactionDTO>>

    @POST("api/wallet/recharge")
    suspend fun recharge(@Body request: TopUpRequest): Response<RechargeResponse>

    @POST("api/wallet/bank-info")
    suspend fun saveBankInfo(@Body request: CreateBankInfoRequest): Response<BankInfoResponse>

    @GET("api/wallet/bank-info")
    suspend fun getBankInfo(): Response<BankInfoResponse>

    @POST("api/wallet/withdraw")
    suspend fun withdraw(@Body request: WithdrawRequest): Response<WithdrawResponse>
}
