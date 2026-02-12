package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.exception.CheckProfileUpdateFailure;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
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
import org.springframework.mail.javamail.JavaMailSender;
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

@WebMvcTest(ProfileWebController.class)
class ProfileControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private UserUpdateService userUpdateService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private JwtFilter jwtFilter;

    private User testUser;

    @Autowired
    public ProfileControllerTest(MockMvc mockMvc) {
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

        // Bypass JWT filter
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

        // Configuration de l'authentification
        when(authPort.extractUsername()).thenReturn(testUser.username());
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
            verify(authPort).extractUsername();
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
                    testUser.role(),
                    testUser.status(),
                    testUser.authMode(),
                    testUser.createdAt(),
                    testUser.updatedAt(),
                    false
            );

            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            when(userUpdateService.updateProfile("testuser","newusername", "NewFirst", "NewLast"))
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

            verify(userUpdateService).updateProfile("testuser","newusername", "NewFirst", "NewLast");
            verify(authPort).refreshAuthentication(updatedUser);
        }

        @Test
        @WithMockUser(username = "testuser")
        void updateProfile_ShouldReturnProfileView_WhenUsernameAlreadyTaken() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            when(userUpdateService.updateProfile("testuser", "takenusername", "First", "Last"))
                    .thenThrow(new CheckProfileUpdateFailure("Ce nom d'utilisateur est déjà pris"));

            // When & Then
            mockMvc.perform(post("/profile/update")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "First")
                            .param("lastName", "Last")
                            .param("username", "takenusername"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(view().name("redirect:/profile"))
                    .andExpect(flash().attributeExists("error"));

            verify(authPort, never()).refreshAuthentication(any());
        }
    }

    @Nested
    class UpdateEmailTests {
        @Test
        @WithMockUser(username = "testuser")
        void updateEmail_ShouldRedirectToConfirmAction_WhenSuccessful() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            doNothing().when(userUpdateService).requestEmailChange("testuser", "new@example.com", "correctPassword", "correctPassword");

            // When & Then
            mockMvc.perform(post("/profile/request-email-change")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("newEmail", "new@example.com")
                            .param("currentPassword", "correctPassword")
                            .param("newPassword", "correctPassword")
                            .param("confirmPassword", "correctPassword"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile/confirm-action"))
                    .andExpect(flash().attribute("actionType", "EMAIL_CHANGE"))
                    .andExpect(flash().attributeExists("infoMessage"));

            verify(userUpdateService).requestEmailChange("testuser", "new@example.com", "correctPassword", "correctPassword");
        }

        @Test
        @WithMockUser(username = "testuser")
        void updateEmail_ShouldRedirectToProfile_WhenPasswordIncorrect() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            doThrow(new CheckProfileUpdateFailure("Incorrect password"))
                    .when(userUpdateService).requestEmailChange("testuser", "new@example.com", "wrongPassword", "wrongPassword");

            // When & Then
            mockMvc.perform(post("/profile/request-email-change")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("newEmail", "new@example.com")
                            .param("currentPassword", "wrongPassword")
                            .param("newPassword", "wrongPassword")
                            .param("confirmPassword", "wrongPassword"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    class UpdatePasswordTests {
        @Test
        @WithMockUser(username = "testuser")
        void updatePassword_ShouldRedirectToConfirmAction_WhenSuccessful() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            doNothing().when(userUpdateService).requestPasswordChange("testuser", "currentPass", "newPassword123");

            // When & Then
            mockMvc.perform(post("/profile/request-password-change")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("currentPassword", "currentPass")
                            .param("newPassword", "newPassword123")
                            .param("confirmPassword", "newPassword123"))
                    .andExpect(status().is3xxRedirection())
                    // Redirection attendue vers la page de confirmation
                    .andExpect(redirectedUrl("/profile/confirm-action"))
                    .andExpect(flash().attribute("actionType", "PASSWORD_CHANGE"))
                    .andExpect(flash().attributeExists("infoMessage"));

            verify(userUpdateService).requestPasswordChange(testUser.username(), "currentPass", "newPassword123");
        }

        @Test
        @WithMockUser(username = "testuser")
        void updatePassword_ShouldReturnProfileView_WhenCurrentPasswordIncorrect() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);
            doThrow(new CheckProfileUpdateFailure("Mot de passe actuel incorrect"))
                    .when(userUpdateService).requestPasswordChange("testuser", "wrongPassword", "newPassword123");

            // When & Then
            mockMvc.perform(post("/profile/request-password-change")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("currentPassword", "wrongPassword")
                            .param("newPassword", "newPassword123")
                            .param("confirmPassword", "newPassword123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(view().name("redirect:/profile"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void updatePassword_ShouldReturnProfileView_WhenPasswordsDontMatch() throws Exception {
            // Given
            when(userQueryService.getUserByUsername("testuser")).thenReturn(testUser);

            // When & Then
            mockMvc.perform(post("/profile/request-password-change")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("currentPassword", "currentPass")
                            .param("newPassword", "newPassword123")
                            .param("confirmPassword", "differentPassword"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(view().name("redirect:/profile"))
                    // Votre contrôleur définit bien "error" dans ce cas précis
                    .andExpect(flash().attributeExists("error"));

            verify(userUpdateService, never()).requestPasswordChange(any(), any(), any());
        }
    }
}