package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.PasswordHasher;
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
    private PasswordHasher passwordHasher;
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
            mockMvc.perform(put("/api/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"","lastName":"Dupont","username":"viewer"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userUpdateService, never()).updateProfile(any(), any(), any(), any());
        }
    }

    // ========== POST /api/account/request-password-change ==========

    @Nested
    class RequestPasswordChange {

        @Test
        void shouldRequestChangeSuccessfully() throws Exception {
            doNothing().when(userUpdateService).requestPasswordChange(any(), any(), any());

            mockMvc.perform(post("/api/account/request-password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"old","newPassword":"newPass1!","confirmPassword":"newPass1!"}
                                    """))
                    .andExpect(status().isOk());

            verify(userUpdateService).requestPasswordChange("viewer", "old", "newPass1!");
        }

        @Test
        void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
            mockMvc.perform(post("/api/account/request-password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"old","newPassword":"newPass1!","confirmPassword":"different"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userUpdateService, never()).requestPasswordChange(any(), any(), any());
        }
    }

    // ========== POST /api/account/confirm-password-change ==========

    @Nested
    class ConfirmPasswordChange {

        @Test
        void shouldConfirmChangeSuccessfully() throws Exception {
            var user = buildUser("viewer");
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            when(passwordHasher.hash("newPass1!")).thenReturn("hashed");
            doNothing().when(emailVerificationService).confirmPasswordChange(any(), any(), any());

            mockMvc.perform(post("/api/account/confirm-password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"code":"123456","newPassword":"newPass1!","confirmPassword":"newPass1!"}
                                    """))
                    .andExpect(status().isOk());

            verify(emailVerificationService).confirmPasswordChange(user.id(), "123456", "hashed");
        }

        @Test
        void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
            mockMvc.perform(post("/api/account/confirm-password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"code":"123456","newPassword":"newPass1!","confirmPassword":"different"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(emailVerificationService, never()).confirmPasswordChange(any(), any(), any());
        }
    }

    // ========== POST /api/account/request-email-change ==========

    @Nested
    class RequestEmailChange {

        @Test
        void shouldRequestEmailChangeForLocalUser() throws Exception {
            var user = buildUser("viewer");
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            doNothing().when(userUpdateService).requestEmailChange(any(), any(), any(), any());

            mockMvc.perform(post("/api/account/request-email-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newEmail":"new@example.com","currentPassword":"pass","newPassword":"","confirmPassword":""}
                                    """))
                    .andExpect(status().isOk());

            verify(userUpdateService).requestEmailChange("viewer", "new@example.com", "pass", "");
        }
    }

    // ========== POST /api/account/confirm-email-change ==========

    @Nested
    class ConfirmEmailChange {

        @Test
        void shouldConfirmEmailChangeSuccessfully() throws Exception {
            var user = buildUser("viewer");
            var updated = buildUser("viewer");
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            when(emailVerificationService.confirmEmailChange(user.id(), "654321")).thenReturn(updated);

            mockMvc.perform(post("/api/account/confirm-email-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"code":"654321"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.username").value("viewer"));
        }
    }

    // ========== POST /api/account/resend-confirmation ==========

    @Nested
    class ResendConfirmation {

        @Test
        void shouldResendConfirmationWhenEmailNotVerified() throws Exception {
            var user = new User(UUID.randomUUID(), "viewer", "Jean", "Dupont", "jean@example.com",
                    UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, false);
            when(userService.getUserByUsername("viewer")).thenReturn(user);
            doNothing().when(emailVerificationService).sendEmailConfirmation(any(), any());

            mockMvc.perform(post("/api/account/resend-confirmation"))
                    .andExpect(status().isOk());

            verify(emailVerificationService).sendEmailConfirmation(user.id(), "jean@example.com");
        }

        @Test
        void shouldReturnOkWithoutResendingWhenEmailAlreadyVerified() throws Exception {
            var user = buildUser("viewer"); // emailVerified = true
            when(userService.getUserByUsername("viewer")).thenReturn(user);

            mockMvc.perform(post("/api/account/resend-confirmation"))
                    .andExpect(status().isOk());

            verify(emailVerificationService, never()).sendEmailConfirmation(any(), any());
        }
    }

    // ========== POST /api/account/set-password ==========

    @Nested
    class SetPassword {

        @Test
        void shouldSetPasswordSuccessfully() throws Exception {
            doNothing().when(userUpdateService).setPasswordForOAuthUser(any(), any());

            mockMvc.perform(post("/api/account/set-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"NewPass1!","confirmPassword":"NewPass1!"}
                                    """))
                    .andExpect(status().isOk());

            verify(userUpdateService).setPasswordForOAuthUser("viewer", "NewPass1!");
        }

        @Test
        void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
            mockMvc.perform(post("/api/account/set-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"NewPass1!","confirmPassword":"different"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userUpdateService, never()).setPasswordForOAuthUser(any(), any());
        }
    }
}
