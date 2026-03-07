package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.presentation.dto.user.TopUpRequestDTO;
import fr.uge.forkeat.presentation.dto.user.UserDTO;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.TransactionType;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.BankInfoService;
import fr.uge.forkeat.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class WalletRestControllerSecueirtyTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BankInfoService bankInfoService;


    // Test fixtures
    private static final String USERNAME = "testuser";
    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final Long BALANCE = 5000L;
    private static final String PAYMENT_URL = "https://payment.example.com/pay";
    private static final String PAYOUT_ID = "payout_abc123";


    private static ObjectMapper objectMapper = new ObjectMapper();

    private UserDTO mockUser;
    private BankInfo mockBankInfo;
    private List<Transaction> mockTransactions;

    @BeforeEach
    void setup() {
        // User mock

        // BankInfo mock
        mockBankInfo = new BankInfo(UUID.randomUUID(), "BNP Paribas", "BNPAFRPP");

        // Transactions mock
        mockTransactions = List.of();

        // Auth mock — commun à tous les endpoints
        when(authPort.extractUsername()).thenReturn(USERNAME);
        when(userService.getUserByUsername(USERNAME)).thenReturn(createUser(UUID.randomUUID()));

        // Wallet mocks
        when(walletService.prepareTopUp(any(), any(), any(), any()))
                .thenReturn(PAYMENT_URL);
        when(walletService.getBalance(any()))
                .thenReturn(BALANCE);
        when(walletService.getTransactionHistory(any()))
                .thenReturn(mockTransactions);
        when(walletService.requestWithdrawal(any(), any()))
                .thenReturn(PAYOUT_ID);

        // BankInfo mocks
        when(bankInfoService.createOrUpdateBankInfo(any(), any(), any(), any(), any()))
                .thenReturn(mockBankInfo);
        when(bankInfoService.getBankInfoByUserId(any()))
                .thenReturn(Optional.of(mockBankInfo));
    }


    @Nested
    class WalletnRestControllerSecurityTest{



        @Test
        void testRecharge() throws Exception {
            testRights(post("/api/wallet/recharge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new TopUpRequestDTO(101L, "src"))), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testGetBalance() throws Exception {
            testRights(get("/api/wallet/balance"), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testGetTransactions() throws Exception {
            testRights(get("/api/wallet/transactions"), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testCreateOrUpdateBankInfo() throws Exception {
            testRights(post("/api/wallet/bank-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
            {"bankName": "BNP Paribas", "iban": "FR7630006000011234567890189", "bic": "BNPAFRPP"}
        """), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testGetBankInfo() throws Exception {
            testRights(get("/api/wallet/bank-info"), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testRequestWithdrawal() throws Exception {
            testRights(post("/api/wallet/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
            {"amount": 500}
        """), AuthorizationTest.EMAIL_VERIFIED);
        }
    }
    private void testRights(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expected = status().isForbidden();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().is2xxSuccessful() : status().isUnauthorized();
        mockMvc.perform(requestBuilders).andExpect(expectedForUnauthenticated);

        if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


        if(authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }

    private User createUser(UUID id) {
        return new User(id, "PaxGPT", "Pax", "Pekpa", "a@gmail.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }
}

