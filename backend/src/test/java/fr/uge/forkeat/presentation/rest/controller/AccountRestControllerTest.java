package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.exception.CheckProfileUpdateFailureException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import fr.uge.forkeat.service.exception.WalletNotFoundException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountRestControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private AuthenticationPort authPort;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserUpdateService userUpdateService;
    @MockitoBean
    private EmailVerificationService emailVerificationService;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    AccountRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        when(authPort.extractUsername()).thenReturn("viewer");
    }

    private User buildUser(String username) {
        return new User(UUID.randomUUID(), username, "Jean", "Dupont", "jean@example.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }

    // ========== GET /api/account ==========

    @Nested
    class GetAccount {

        @Test
        void shouldReturnCurrentUserInfo() throws Exception {
            when(userService.getUserByUsername("viewer")).thenReturn(buildUser("viewer"));

            mockMvc.perform(get("/api/account"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.username").value("viewer"));
        }

        @Test
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userService.getUserByUsername(any()))
                    .thenThrow(new ResourceNotFoundException("User not found: viewer"));

            mockMvc.perform(get("/api/account"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== PUT /api/account ==========

    @Nested
    class UpdateProfile {

        @Test
        void shouldUpdateProfileSuccessfully() throws Exception {
            var updated = buildUser("new_username");
            when(userUpdateService.updateProfile(eq("viewer"), eq("new_username"), eq("Jean"), eq("Dupont")))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Jean","lastName":"Dupont","username":"new_username"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.username").value("new_username"));
        }

        @Test
        void shouldReturn400WhenFieldIsBlank() throws Exception {
            when(userUpdateService.updateProfile(eq("viewer"), eq("viewer"), eq(""), eq("Dupont")))
                    .thenThrow(new CheckProfileUpdateFailureException("Veuillez remplir tous les champs"));

            mockMvc.perform(put("/api/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"","lastName":"Dupont","username":"viewer"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenUsernameAlreadyTaken() throws Exception {
            when(userUpdateService.updateProfile(eq("viewer"), eq("taken"), any(), any()))
                    .thenThrow(new CheckProfileUpdateFailureException("Ce nom d'utilisateur est déjà pris"));

            mockMvc.perform(put("/api/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Jean","lastName":"Dupont","username":"taken"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /api/account/password-change-requests ==========

    @Nested
    class RequestPasswordChange {

        @Test
        void shouldRequestChangeSuccessfully() throws Exception {
            doNothing().when(userUpdateService).requestPasswordChange(any(), any(), any(), any());

            mockMvc.perform(post("/api/account/password-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"old","newPassword":"newPass1!","confirmPassword":"newPass1!"}
                                    """))
                    .andExpect(status().isOk());

            verify(userUpdateService).requestPasswordChange("viewer", "old", "newPass1!", "newPass1!");
        }

        @Test
        void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("Les mots de passe ne correspondent pas"))
                    .when(userUpdateService).requestPasswordChange("viewer", "old", "newPass1!", "different");

            mockMvc.perform(post("/api/account/password-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"old","newPassword":"newPass1!","confirmPassword":"different"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenCurrentPasswordIncorrect() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("Incorrect current password"))
                    .when(userUpdateService).requestPasswordChange("viewer", "wrong", "newPass1!", "newPass1!");

            mockMvc.perform(post("/api/account/password-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"wrong","newPassword":"newPass1!","confirmPassword":"newPass1!"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenNewPasswordTooShort() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("New password must be at least 8 characters"))
                    .when(userUpdateService).requestPasswordChange("viewer", "old", "short", "short");

            mockMvc.perform(post("/api/account/password-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"old","newPassword":"short","confirmPassword":"short"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== PUT /api/account/password-change-requests ==========

    @Nested
    class ConfirmPasswordChange {

        @Test
        void shouldConfirmChangeSuccessfully() throws Exception {
            var user = buildUser("viewer");
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            doNothing().when(emailVerificationService).confirmPasswordChange(user.id(), "123456");

            mockMvc.perform(put("/api/account/password-change-requests")
                            .param("code", "123456"))
                    .andExpect(status().isOk());

            verify(emailVerificationService).confirmPasswordChange(user.id(), "123456");
        }

        @Test
        void shouldReturn400WhenCodeIsInvalid() throws Exception {
            var user = buildUser("viewer");
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            doThrow(new VerificationException("Invalid or expired code"))
                    .when(emailVerificationService).confirmPasswordChange(user.id(), "000000");

            mockMvc.perform(put("/api/account/password-change-requests")
                            .param("code", "000000"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /api/account/email-change-requests ==========

    @Nested
    class RequestEmailChange {

        @Test
        void shouldRequestEmailChangeForLocalUser() throws Exception {
            doNothing().when(userUpdateService).requestEmailChange(any(), any(), any(), any(), any());

            mockMvc.perform(post("/api/account/email-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newEmail":"new@example.com","currentPassword":"pass","newPassword":"","confirmPassword":""}
                                    """))
                    .andExpect(status().isOk());

            verify(userUpdateService).requestEmailChange("viewer", "new@example.com", "pass", "", "");
        }

        @Test
        void shouldRequestEmailChangeForGoogleUser() throws Exception {
            doNothing().when(userUpdateService).requestEmailChange(any(), any(), any(), any(), any());

            mockMvc.perform(post("/api/account/email-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newEmail":"new@example.com","currentPassword":"","newPassword":"NewPass1!","confirmPassword":"NewPass1!"}
                                    """))
                    .andExpect(status().isOk());

            verify(userUpdateService).requestEmailChange("viewer", "new@example.com", "", "NewPass1!", "NewPass1!");
        }

        @Test
        void shouldReturn400WhenCurrentPasswordIncorrect() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("Incorrect password"))
                    .when(userUpdateService).requestEmailChange("viewer", "new@example.com", "wrong", "", "");

            mockMvc.perform(post("/api/account/email-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newEmail":"new@example.com","currentPassword":"wrong","newPassword":"","confirmPassword":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenEmailAlreadyTaken() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("Cet email est déjà utilisé"))
                    .when(userUpdateService).requestEmailChange("viewer", "taken@example.com", "pass", "", "");

            mockMvc.perform(post("/api/account/email-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newEmail":"taken@example.com","currentPassword":"pass","newPassword":"","confirmPassword":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenGoogleUserPasswordTooShort() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("New password must be at least 8 characters"))
                    .when(userUpdateService).requestEmailChange("viewer", "new@example.com", "", "short", "short");

            mockMvc.perform(post("/api/account/email-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newEmail":"new@example.com","currentPassword":"","newPassword":"short","confirmPassword":"short"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenGoogleUserPasswordsDontMatch() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("Les mots de passe ne correspondent pas"))
                    .when(userUpdateService).requestEmailChange("viewer", "new@example.com", "", "newPass1!", "different");

            mockMvc.perform(post("/api/account/email-change-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newEmail":"new@example.com","currentPassword":"","newPassword":"newPass1!","confirmPassword":"different"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== PUT /api/account/email-change-requests ==========

    @Nested
    class ConfirmEmailChange {

        @Test
        void shouldConfirmEmailChangeSuccessfully() throws Exception {
            var user = buildUser("viewer");
            var updated = buildUser("viewer");
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            when(emailVerificationService.confirmEmailChange(user.id(), "654321")).thenReturn(updated);

            mockMvc.perform(put("/api/account/email-change-requests")
                            .param("code", "654321"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.username").value("viewer"));
        }

        @Test
        void shouldReturn400WhenCodeIsInvalid() throws Exception {
            var user = buildUser("viewer");
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            when(emailVerificationService.confirmEmailChange(user.id(), "000000"))
                    .thenThrow(new VerificationException("Invalid or expired code"));

            mockMvc.perform(put("/api/account/email-change-requests")
                            .param("code", "000000"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /api/account/email-confirmations ==========

    @Nested
    class ResendConfirmation {

        @Test
        void shouldResendConfirmationWhenEmailNotVerified() throws Exception {
            var user = new User(UUID.randomUUID(), "viewer", "Jean", "Dupont", "jean@example.com",
                    UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, false);
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            doNothing().when(emailVerificationService).sendEmailConfirmation(any(), any());

            mockMvc.perform(post("/api/account/email-confirmations"))
                    .andExpect(status().isOk());

            verify(emailVerificationService).sendEmailConfirmation(user.id(), "jean@example.com");
        }

        @Test
        void shouldReturnOkWithoutResendingWhenEmailAlreadyVerified() throws Exception {
            var user = buildUser("viewer"); // emailVerified = true
            when(userService.getUserByUsername("viewer")).thenReturn(user);

            mockMvc.perform(post("/api/account/email-confirmations"))
                    .andExpect(status().isOk());

            verify(emailVerificationService, never()).sendEmailConfirmation(any(), any());
        }
    }

    // ========== PUT /api/account/password ==========

    @Nested
    class SetPassword {

        @Test
        void shouldSetPasswordSuccessfully() throws Exception {
            doNothing().when(userUpdateService).setPasswordForOAuthUser(any(), any(), any());

            mockMvc.perform(put("/api/account/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"NewPass1!","confirmPassword":"NewPass1!"}
                                    """))
                    .andExpect(status().isOk());

            verify(userUpdateService).setPasswordForOAuthUser("viewer", "NewPass1!", "NewPass1!");
        }

        @Test
        void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("Les mots de passe ne correspondent pas"))
                    .when(userUpdateService).setPasswordForOAuthUser("viewer", "NewPass1!", "different");

            mockMvc.perform(put("/api/account/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"NewPass1!","confirmPassword":"different"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenPasswordDoesNotMeetComplexityRequirements() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("New password must be at least 8 characters"))
                    .when(userUpdateService).setPasswordForOAuthUser("viewer", "short", "short");

            mockMvc.perform(put("/api/account/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"short","confirmPassword":"short"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenUserAlreadyHasLocalPassword() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("This user already has a local password"))
                    .when(userUpdateService).setPasswordForOAuthUser("viewer", "NewPass1!", "NewPass1!");

            mockMvc.perform(put("/api/account/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"NewPass1!","confirmPassword":"NewPass1!"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== DELETE /api/account ==========

    @Nested
    class DeleteAccount {

        @Test
        void shouldDeleteAccountSuccessfully() throws Exception {
            doNothing().when(userService).deleteAccount("viewer");

            mockMvc.perform(delete("/api/account"))
                    .andExpect(status().isOk());

            verify(userService).deleteAccount("viewer");
        }

        @Test
        void shouldReturn404WhenUserNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found: viewer"))
                    .when(userService).deleteAccount("viewer");

            mockMvc.perform(delete("/api/account"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn404WhenWalletNotFound() throws Exception {
            doThrow(new WalletNotFoundException(UUID.randomUUID()))
                    .when(userService).deleteAccount("viewer");

            mockMvc.perform(delete("/api/account"))
                    .andExpect(status().isNotFound());
        }
    }
}