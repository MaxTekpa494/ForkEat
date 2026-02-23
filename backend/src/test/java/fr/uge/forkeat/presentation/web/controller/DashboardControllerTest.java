package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test pour DashboardController.
 * Ce controller gère uniquement l'endpoint /dashboard.
 */
@WebMvcTest(DashboardWebController.class)
class DashboardControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;


    private User testUser;

    @Autowired
    public DashboardControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }


    @BeforeEach
    void setUp() throws Exception {
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

        // Configuration de l'authentification
        when(authPort.extractUsername()).thenReturn(testUser.username());
    }

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_ShouldReturnDashboardView_WhenAuthenticated() throws Exception {
        // Given
        Long balance = 1000L;
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(balance);

        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("balance"))
                .andExpect(model().attribute("user", testUser))
                .andExpect(model().attribute("balance", balance));

        verify(userQueryService).getUserByUsername("testuser");
        verify(walletService).getBalance(testUser.id());
    }

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_ShouldThrowException_WhenUserNotFound() throws Exception {
        // Given
        when(userQueryService.getUserByUsername("testuser"))
                .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is4xxClientError());

        verify(userQueryService).getUserByUsername("testuser");
        verify(walletService, never()).getBalance(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_ShouldLoadUserData_WithCorrectUsername() throws Exception {
        // Given
        Long balance = 500L;
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(balance);

        // When
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());

        // Then
        verify(authPort).extractUsername();
        verify(userQueryService).getUserByUsername("testuser");
    }

    @Test
    @WithMockUser(username = "otheruser")
    void dashboard_ShouldLoadCorrectUser_BasedOnAuthentication() throws Exception {
        // Given
        var otherUser = new User(
                UUID.randomUUID(),
                "otheruser",
                "Jane",
                "Smith",
                "other@example.com",
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                Instant.now(),
                Instant.now(),
                false);

        when(authPort.extractUsername()).thenReturn("otheruser");
        when(userQueryService.getUserByUsername("otheruser")).thenReturn(otherUser);
        when(walletService.getBalance(otherUser.id())).thenReturn(2000L);

        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("user", otherUser));

        verify(userQueryService).getUserByUsername("otheruser");
        verify(walletService).getBalance(otherUser.id());
    }

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_ShouldDisplayCorrectBalance() throws Exception {
        // Given
        Long expectedBalance = 12345L;
        when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
        when(walletService.getBalance(testUser.id())).thenReturn(expectedBalance);

        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("balance", expectedBalance));

        verify(walletService).getBalance(testUser.id());
    }
}