package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.user.UserUpdateService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test pour ProfileController.
 * Ce controller gère /profile et ses sous-routes.
 */
@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private UserUpdateService userUpdateService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtFilter jwtFilter;

    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new User(
                UUID.randomUUID(),
                "testuser",
                "John",
                "Doe",
                "test@example.com",
                "hashedPassword",
                Instant.now(),
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                UUID.randomUUID()
        );

        // Bypass JWT filter
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

        // Configuration de l'authentification
        when(authPort.extractUsername(any())).thenReturn(testUser.username());
    }

    @Nested
    class ProfileTests {
        @Test
        @WithMockUser(username = "testuser")
        void profile_ShouldReturnProfileView_WhenAuthenticated() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);

            // When & Then
            mockMvc.perform(get("/profile"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/profile"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attribute("user", testUser))
                    .andExpect(model().attribute("pageTitle", "Mon Profil - ForkEat"));

            verify(userQueryService).getUserByUsername("testuser");
        }

        @Test
        @WithMockUser(username = "testuser")
        void profile_ShouldExtractUsernameFromAuthentication() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);

            // When
            mockMvc.perform(get("/profile"))
                    .andExpect(status().isOk());

            // Then
            verify(authPort).extractUsername(any());
            verify(userQueryService).getUserByUsername("testuser");
        }
    }

    @Nested
    class UpdateProfileTests {
        @Test
        @WithMockUser(username = "testuser")
        void updateProfile_ShouldRedirectToProfile_WhenSuccessful() throws Exception {
            // Given
            var updatedUser = new User(
                    testUser.id(),
                    "newusername",
                    "NewFirst",
                    "NewLast",
                    testUser.email(),
                    testUser.password(),
                    testUser.createdAt(),
                    testUser.role(),
                    testUser.status(),
                    testUser.authentificationMode(),
                    testUser.walletId()
            );

            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            when(userUpdateService.updateProfile(testUser.id(), "NewFirst", "NewLast", "newusername"))
                    .thenReturn(updatedUser);
            doNothing().when(authPort).refreshAuthentication(updatedUser);

            // When & Then
            mockMvc.perform(post("/profile/update")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "NewFirst")
                            .param("lastName", "NewLast")
                            .param("username", "newusername"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile"))
                    .andExpect(flash().attributeExists("success"));

            verify(userUpdateService).updateProfile(testUser.id(), "NewFirst", "NewLast", "newusername");
            verify(authPort).refreshAuthentication(updatedUser);
        }

        @Test
        @WithMockUser(username = "testuser")
        void updateProfile_ShouldReturnProfileView_WhenUsernameAlreadyTaken() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            when(userUpdateService.updateProfile(testUser.id(), "First", "Last", "takenusername"))
                    .thenThrow(new IllegalArgumentException("Ce nom d'utilisateur est déjà pris"));

            // When & Then
            mockMvc.perform(post("/profile/update")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "First")
                            .param("lastName", "Last")
                            .param("username", "takenusername"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/profile"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Ce nom d'utilisateur est déjà pris"));

            verify(authPort, never()).refreshAuthentication(any());
        }
    }

    @Nested
    class UpdateEmailTests {
        @Test
        @WithMockUser(username = "testuser")
        void updateEmail_ShouldRedirectToLogout_WhenSuccessful() throws Exception {
            // Given
            var updatedUser = new User(
                    testUser.id(),
                    testUser.username(),
                    testUser.firstName(),
                    testUser.lastName(),
                    "new@example.com",
                    testUser.password(),
                    testUser.createdAt(),
                    testUser.role(),
                    testUser.status(),
                    testUser.authentificationMode(),
                    testUser.walletId()
            );

            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            when(userUpdateService.updateEmail(testUser.id(), "new@example.com", "correctPassword"))
                    .thenReturn(updatedUser);

            // When & Then
            mockMvc.perform(post("/profile/update-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("newEmail", "new@example.com")
                            .param("currentPassword", "correctPassword"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/logout"))
                    .andExpect(flash().attributeExists("success"));

            verify(userUpdateService).updateEmail(testUser.id(), "new@example.com", "correctPassword");
        }

        @Test
        @WithMockUser(username = "testuser")
        void updateEmail_ShouldRedirectToProfile_WhenPasswordIncorrect() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            when(userUpdateService.updateEmail(testUser.id(), "new@example.com", "wrongPassword"))
                    .thenThrow(new IllegalArgumentException("Mot de passe incorrect"));

            // When & Then
            mockMvc.perform(post("/profile/update-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("newEmail", "new@example.com")
                            .param("currentPassword", "wrongPassword"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    class UpdatePasswordTests {
        @Test
        @WithMockUser(username = "testuser")
        void updatePassword_ShouldRedirectToProfile_WhenSuccessful() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            doNothing().when(userUpdateService).updatePassword(testUser.id(), "currentPass", "newPassword123");

            // When & Then
            mockMvc.perform(post("/profile/update-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("currentPassword", "currentPass")
                            .param("newPassword", "newPassword123")
                            .param("confirmPassword", "newPassword123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile"))
                    .andExpect(flash().attributeExists("success"));

            verify(userUpdateService).updatePassword(testUser.id(), "currentPass", "newPassword123");
        }

        @Test
        @WithMockUser(username = "testuser")
        void updatePassword_ShouldReturnProfileView_WhenCurrentPasswordIncorrect() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            doThrow(new IllegalArgumentException("Mot de passe actuel incorrect"))
                    .when(userUpdateService).updatePassword(testUser.id(), "wrongPassword", "newPassword123");

            // When & Then
            mockMvc.perform(post("/profile/update-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("currentPassword", "wrongPassword")
                            .param("newPassword", "newPassword123")
                            .param("confirmPassword", "newPassword123"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/profile"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void updatePassword_ShouldReturnProfileView_WhenPasswordsDontMatch() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);

            // When & Then
            mockMvc.perform(post("/profile/update-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("currentPassword", "currentPass")
                            .param("newPassword", "newPassword123")
                            .param("confirmPassword", "differentPassword"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/profile"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Les mots de passe ne correspondent pas"));

            verify(userUpdateService, never()).updatePassword(any(), any(), any());
        }
    }
}