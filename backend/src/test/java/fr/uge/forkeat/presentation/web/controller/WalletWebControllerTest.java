package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.user.BankInfoService;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.TransactionStatus; // New import
import fr.uge.forkeat.service.model.TransactionType;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletWebControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private BankInfoService bankInfoService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @Autowired
    public WalletWebControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        testUser = new User(
                UUID.randomUUID(),
                "testuser",
                "John",
                "Doe",
                "test@example.com",
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                Instant.now(),
                Instant.now(),
                false
        );

        when(authPort.extractUsername()).thenReturn("testuser");
        when(bankInfoService.getBankInfoByUserId(any())).thenReturn(Optional.empty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldReturnWalletView_WhenAuthenticated() throws Exception {
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(5000L);
        when(walletService.getTransactionHistory(testUser.id())).thenReturn(List.of());

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(view().name("wallet/index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("balance"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attribute("pageTitle", "Mon Wallet - ForkEat"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldDisplayCorrectBalance() throws Exception {
        Long expectedBalance = 12500L;
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(expectedBalance);
        when(walletService.getTransactionHistory(testUser.id())).thenReturn(List.of());

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("balance", expectedBalance));
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldDisplayZeroBalance_WhenNoFunds() throws Exception {
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(0L);
        when(walletService.getTransactionHistory(testUser.id())).thenReturn(List.of());

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("balance", 0L));
    }

    @Test
    @WithMockUser(username = "unknownuser")
    void walletPage_ShouldThrow_WhenUserNotFound() throws Exception {
        when(authPort.extractUsername()).thenReturn("unknownuser");

        when(userQueryService.getUserByUsername("unknownuser"))
                .thenThrow(new ResourceNotFoundException("Utilisateur non trouve"));

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldSetPaymentSuccess_WhenQueryParam() throws Exception {
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(5000L);
        when(walletService.getTransactionHistory(testUser.id())).thenReturn(List.of());

        mockMvc.perform(get("/wallet").param("payment", "success"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("paymentSuccess", true));
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldSetPaymentCancel_WhenQueryParam() throws Exception {
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(5000L);
        when(walletService.getTransactionHistory(testUser.id())).thenReturn(List.of());

        mockMvc.perform(get("/wallet").param("payment", "cancel"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("paymentCancel", true));
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldDisplayTransactions() throws Exception {
        var walletId = UUID.randomUUID();
        // Updated Transaction constructor calls
        var transactions = List.of(
                new Transaction(UUID.randomUUID(), null, walletId, 1000L, TransactionType.RECHARGE, Instant.now(), "tx_1", TransactionStatus.SUCCEEDED),
                new Transaction(UUID.randomUUID(), null, walletId, 500L, TransactionType.RECHARGE, Instant.now(), "tx_2", TransactionStatus.SUCCEEDED)
        );
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(1500L);
        when(walletService.getTransactionHistory(testUser.id())).thenReturn(transactions);

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("transactions", transactions));
    }

    @Test
    @WithMockUser(username = "testuser")
    void recharge_ShouldRedirectToStripe() throws Exception {
        var expectedUrl = "https://checkout.stripe.com/pay/abc123";
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.prepareTopUp(eq(testUser.id()), eq("test@example.com"), eq(1000L), eq(null)))
                .thenReturn(expectedUrl);

        mockMvc.perform(post("/wallet/recharge").param("amount", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedUrl));
    }
}