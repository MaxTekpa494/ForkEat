package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.presentation.dto.user.TopUpRequestDTO;
import fr.uge.forkeat.presentation.dto.user.CreateBankInfoRequestDTO; // New import
import fr.uge.forkeat.presentation.dto.user.WithdrawalRequestDTO;     // New import
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.WithdrawalException;        // New import
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.transaction.Transaction;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import fr.uge.forkeat.service.model.user.BankInfo;                   // New import
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.BankInfoService;                  // New import
import fr.uge.forkeat.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletRestControllerIntegrationTest { // Renamed class

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BankInfoService bankInfoService; // New mock

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private UUID bankInfoId;
    private BankInfo testBankInfo;

    @Autowired
    public WalletRestControllerIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        testUser = new User(
                UUID.randomUUID(), "testuser", "John", "Doe", "test@test.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), true
        );
        bankInfoId = UUID.randomUUID();
        testBankInfo = new BankInfo(testUser.id(), "My Bank", "ext_acct_test123");

        when(authPort.extractUsername()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
    }

    @Test
    void shouldReturnPaymentUrl() throws Exception {
        var request = new TopUpRequestDTO(1000L, null);
        var expectedUrl = "https://checkout.stripe.com/pay/123";

        when(walletService.prepareTopUp(
                eq(testUser.id()),
                eq("test@test.com"),
                eq(1000L),
                eq(null)
        )).thenReturn(expectedUrl);

        mockMvc.perform(post("/api/wallet/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedUrl));
    }

    @Test
    void shouldReturnBalance() throws Exception {
        when(walletService.getBalance(testUser.id())).thenReturn(5000L);

        mockMvc.perform(get("/api/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5000));
    }

    @Test
    void shouldReturnTransactions() throws Exception {
        var transactions = List.of(
                new Transaction(UUID.randomUUID(), null, UUID.randomUUID(), 1000L, TransactionType.RECHARGE, Instant.now(), "tx_1", TransactionStatus.SUCCEEDED) // Updated Transaction constructor
        );
        when(walletService.getTransactionHistory(testUser.id())).thenReturn(transactions);

        mockMvc.perform(get("/api/wallet/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(1000));
    }

    // --- New Tests for Bank Info ---

    @Test
    void shouldCreateOrUpdateBankInfoSuccessfully() throws Exception {
        var request = new CreateBankInfoRequestDTO("New Bank", "FR1234567890123456789012345", "NBANKFRXX");
        var expectedBankInfo = new BankInfo(testUser.id(), request.bankName(), "ext_acct_new");

        when(bankInfoService.createOrUpdateBankInfo(eq(testUser.id()), eq(testUser.email()), eq(request.bankName()), eq(request.iban()), eq(request.bic())))
                .thenReturn(expectedBankInfo);

        mockMvc.perform(post("/api/wallet/bank-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(testUser.id().toString()))
                .andExpect(jsonPath("$.bankName").value("New Bank"))
                .andExpect(jsonPath("$.externalAccountId").value("ext_acct_new"));
    }

    @Test
    void shouldReturnBankInfoSuccessfully() throws Exception {
        when(bankInfoService.getBankInfoByUserId(eq(testUser.id()))).thenReturn(java.util.Optional.of(testBankInfo));

        mockMvc.perform(get("/api/wallet/bank-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.id().toString()))
                .andExpect(jsonPath("$.bankName").value(testBankInfo.bankName()))
                .andExpect(jsonPath("$.externalAccountId").value(testBankInfo.externalAccountId()));
    }

    @Test
    void shouldReturnNotFoundWhenBankInfoDoesNotExist() throws Exception {
        when(bankInfoService.getBankInfoByUserId(eq(testUser.id()))).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/wallet/bank-info"))
                .andExpect(status().isNotFound()); // Based on ResourceNotFoundException
    }

    // --- New Tests for Withdrawal ---

    @Test
    void shouldInitiateWithdrawalSuccessfully() throws Exception {
        var request = new WithdrawalRequestDTO(500L);
        String expectedPayoutId = "payout_stripe_id_xyz";

        when(walletService.requestWithdrawal(eq(testUser.id()), eq(request.amount()))).thenReturn(expectedPayoutId);

        mockMvc.perform(post("/api/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Withdrawal initiated successfully"))
                .andExpect(jsonPath("$.payoutId").value(expectedPayoutId));
    }

    @Test
    void shouldReturnBadRequestWhenWithdrawalFails() throws Exception {
        var request = new WithdrawalRequestDTO(500L);
        String errorMessage = "Insufficient balance for withdrawal.";

        when(walletService.requestWithdrawal(eq(testUser.id()), eq(request.amount())))
                .thenThrow(new WithdrawalException(errorMessage));

        mockMvc.perform(post("/api/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(errorMessage));
    }

//    @Test
//    void shouldReturnBadRequestWhenWithdrawalAmountIsInvalid() throws Exception {
//        // Test with amount <= 0
//        var request = new WithdrawalRequestDTO(0L);
//
//        mockMvc.perform(post("/api/wallet/withdraw")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest()) // Handled by @Valid and MethodArgumentNotValidException
//                .andExpect(jsonPath("$.amount").doesNotExist()); // Or check specific error message
//    }
}