package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletWebControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private AuthenticationPort authPort;

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
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldReturnWalletView_WhenAuthenticated() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(5000L);

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(view().name("wallet/index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("balance"))
                .andExpect(model().attribute("pageTitle", "Mon Wallet - ForkEat"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldDisplayCorrectBalance() throws Exception {
        Long expectedBalance = 12500L;
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(expectedBalance);

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("balance", expectedBalance));
    }

    @Test
    @WithMockUser(username = "testuser")
    void walletPage_ShouldDisplayZeroBalance_WhenNoFunds() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(0L);

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("balance", 0L));
    }

    @Test
    @WithMockUser(username = "unknownuser")
    void walletPage_ShouldThrow_WhenUserNotFound() throws Exception {
        when(authPort.extractUsername()).thenReturn("unknownuser");

        when(userService.getUserByUsername("unknownuser"))
                .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isNotFound());
    }
}