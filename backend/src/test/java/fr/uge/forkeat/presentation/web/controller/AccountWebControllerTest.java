package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.projection.UserAccountDetails;
import fr.uge.forkeat.service.model.user.projection.UserSocialStats;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserUpdateService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountWebController.class)
class AccountWebControllerTest {

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
    private ProfileService profileService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtFilter jwtFilter;

    private User localUser;
    private User googleUser;

    @Autowired
    public AccountWebControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() throws Exception {
        localUser = new User(UUID.randomUUID(), "testuser", "Jean", "Dupont",
                "jean@test.fr", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), false);

        googleUser = new User(UUID.randomUUID(), "googleuser", "Jane", "Doe",
                "jane@gmail.com", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.GOOGLE,
                Instant.now(), Instant.now(), true);

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

        when(authPort.extractUsername()).thenReturn("testuser");
    }

    @Nested
    class AccountPage {

        @Test
        @WithMockUser(username = "testuser")
        void shouldReturnAccountViewWithVm() throws Exception {
            var socialStats = new UserSocialStats(10, 5, 20, 3);
            var vm = new UserAccountDetails(localUser, socialStats, 1000L, 3L);
            when(profileService.getAccountDetails("testuser")).thenReturn(vm);

            mockMvc.perform(get("/account"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("account/index"))
                    .andExpect(model().attributeExists("vm"))
                    .andExpect(model().attribute("vm", vm));

            verify(profileService).getAccountDetails("testuser");
        }

        @Test
        @WithMockUser(username = "testuser")
        void shouldReturn404_WhenUserNotFound() throws Exception {
            when(profileService.getAccountDetails("testuser"))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/account"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdateProfile {

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectWithSuccess_WhenFieldsAreValid() throws Exception {
            var updatedUser = new User(localUser.id(), "newusername", "Nouveau", "Nom",
                    localUser.email(), localUser.role(), localUser.status(), localUser.authMode(),
                    localUser.createdAt(), localUser.updatedAt(), localUser.emailVerified());
            when(userUpdateService.updateProfile("testuser", "newusername", "Nouveau", "Nom"))
                    .thenReturn(updatedUser);

            mockMvc.perform(post("/account/update").with(csrf())
                            .param("firstName", "Nouveau")
                            .param("lastName", "Nom")
                            .param("username", "newusername"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("success"));

            verify(userUpdateService).updateProfile("testuser", "newusername", "Nouveau", "Nom");
            verify(authPort).refreshAuthentication(updatedUser);
        }

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectWithError_WhenUsernameIsEmpty() throws Exception {
            mockMvc.perform(post("/account/update").with(csrf())
                            .param("firstName", "Jean")
                            .param("lastName", "Dupont")
                            .param("username", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("error"));

            verifyNoInteractions(userUpdateService);
        }

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectWithError_WhenFirstNameIsEmpty() throws Exception {
            mockMvc.perform(post("/account/update").with(csrf())
                            .param("firstName", "")
                            .param("lastName", "Dupont")
                            .param("username", "testuser"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("error"));

            verifyNoInteractions(userUpdateService);
        }
    }

    @Nested
    class RequestPasswordChange {

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectToConfirmAction_WhenPasswordsMatch() throws Exception {
            mockMvc.perform(post("/account/request-password-change").with(csrf())
                            .param("currentPassword", "oldPass123")
                            .param("newPassword", "newPass123")
                            .param("confirmPassword", "newPass123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account/confirm-action"))
                    .andExpect(flash().attributeExists("actionType"))
                    .andExpect(flash().attributeExists("infoMessage"));

            verify(userUpdateService).requestPasswordChange("testuser", "oldPass123", "newPass123");
        }

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectWithError_WhenPasswordsDontMatch() throws Exception {
            mockMvc.perform(post("/account/request-password-change").with(csrf())
                            .param("currentPassword", "oldPass123")
                            .param("newPassword", "newPass123")
                            .param("confirmPassword", "different"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("error"));

            verifyNoInteractions(userUpdateService);
        }
    }

    @Nested
    class ConfirmActionPage {

        @Test
        @WithMockUser(username = "testuser")
        void shouldReturnConfirmActionView() throws Exception {
            mockMvc.perform(get("/account/confirm-action"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("account/confirm-action"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void shouldAddActionTypeToModel_WhenProvidedAsQueryParam() throws Exception {
            mockMvc.perform(get("/account/confirm-action")
                            .param("actionType", "PASSWORD_CHANGE"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("actionType", "PASSWORD_CHANGE"));
        }
    }

    @Nested
    class ConfirmPasswordChange {

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectWithSuccess_WhenCodeIsValid() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(localUser);

            mockMvc.perform(post("/account/confirm-password-change").with(csrf())
                            .param("code", "123456"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("success"));

            verify(emailVerificationService).confirmPasswordChange(localUser.id(), "123456");
        }
    }

    @Nested
    class RequestEmailChange {

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectToConfirmAction_ForLocalUser() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(localUser);

            mockMvc.perform(post("/account/request-email-change").with(csrf())
                            .param("newEmail", "new@email.fr")
                            .param("currentPassword", "pass123")
                            .param("newPassword", "")
                            .param("confirmPassword", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account/confirm-action"))
                    .andExpect(flash().attributeExists("actionType"));

            verify(userUpdateService).requestEmailChange("testuser", "new@email.fr", "pass123", "");
        }

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectWithError_WhenGoogleUserPasswordTooShort() throws Exception {
            when(authPort.extractUsername()).thenReturn("googleuser");
            when(userService.getUserByUsername("googleuser")).thenReturn(googleUser);

            mockMvc.perform(post("/account/request-email-change").with(csrf())
                            .param("newEmail", "new@email.fr")
                            .param("currentPassword", "")
                            .param("newPassword", "short")
                            .param("confirmPassword", "short"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("error"));

            verifyNoInteractions(userUpdateService);
        }

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectWithError_WhenGoogleUserPasswordsDontMatch() throws Exception {
            when(authPort.extractUsername()).thenReturn("googleuser");
            when(userService.getUserByUsername("googleuser")).thenReturn(googleUser);

            mockMvc.perform(post("/account/request-email-change").with(csrf())
                            .param("newEmail", "new@email.fr")
                            .param("currentPassword", "")
                            .param("newPassword", "validPass123")
                            .param("confirmPassword", "different123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("error"));

            verifyNoInteractions(userUpdateService);
        }
    }

    @Nested
    class ConfirmEmailChange {

        @Test
        @WithMockUser(username = "testuser")
        void shouldRedirectWithSuccess_AndRefreshAuth() throws Exception {
            var updatedUser = new User(localUser.id(), localUser.username(), localUser.firstName(),
                    localUser.lastName(), "newemail@test.fr", localUser.role(), localUser.status(),
                    localUser.authMode(), localUser.createdAt(), localUser.updatedAt(), true);
            when(userService.getUserByUsername("testuser")).thenReturn(localUser);
            when(emailVerificationService.confirmEmailChange(localUser.id(), "654321"))
                    .thenReturn(updatedUser);

            mockMvc.perform(post("/account/confirm-email-change").with(csrf())
                            .param("code", "654321"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("success"));

            verify(emailVerificationService).confirmEmailChange(localUser.id(), "654321");
            verify(authPort).refreshAuthentication(updatedUser);
        }
    }

    @Nested
    class ResendConfirmation {

        @Test
        @WithMockUser(username = "testuser")
        void shouldSendConfirmation_WhenEmailNotVerified() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(localUser);

            mockMvc.perform(post("/account/resend-confirmation").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("success"));

            verify(emailVerificationService).sendEmailConfirmation(localUser.id(), localUser.email());
        }

        @Test
        @WithMockUser(username = "testuser")
        void shouldSkipSending_WhenEmailAlreadyVerified() throws Exception {
            var verifiedUser = new User(localUser.id(), localUser.username(), localUser.firstName(),
                    localUser.lastName(), localUser.email(), localUser.role(), localUser.status(),
                    localUser.authMode(), localUser.createdAt(), localUser.updatedAt(), true);
            when(userService.getUserByUsername("testuser")).thenReturn(verifiedUser);

            mockMvc.perform(post("/account/resend-confirmation").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"))
                    .andExpect(flash().attributeExists("success"));

            verify(emailVerificationService, never()).sendEmailConfirmation(any(), any());
        }
    }
}