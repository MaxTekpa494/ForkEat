package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.exception.CheckProfileUpdateFailureException;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserUpdateService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
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

@WebMvcTest(AuthWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationPort authenticationPort;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private UserService userService;


    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserUpdateService userUpdateService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    public AuthControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private User createTestUser() {
        return new User(
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
    }

    @Nested
    class LoginPageTests {
        @Test
        void loginPage_ShouldReturnLoginView_WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/auth/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/login"));
        }
    }

    @Nested
    class RegisterPageTests {
        @Test
        void registerPage_ShouldReturnRegisterView_WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/auth/register"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("registerForm"));
        }
    }

    @Nested
    class RegisterTests {
        @Test
        void register_ShouldRedirectToLogin_WhenSuccessful() throws Exception {
            // Given
            var newUser = createTestUser();
            when(userRegistrationService.registerUser(any(UserRegister.class))).thenReturn(newUser);

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "john@example.com")
                            .param("password", "password123")
                            .param("confirmPassword", "password123")
                            .param("terms", "true"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/email-sent"))
                    .andExpect(flash().attributeExists("success"));

            verify(userRegistrationService).registerUser(any(UserRegister.class));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenEmailAlreadyExists() throws Exception {
            when(userRegistrationService.registerUser(any(UserRegister.class)))
                    .thenThrow(new RegisterFailureException("Cet email est déjà utilisé"));

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "existing@example.com")
                            .param("password", "password123")
                            .param("confirmPassword", "password123")
                            .param("terms", "true"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenUsernameAlreadyExists() throws Exception {
            when(userRegistrationService.registerUser(any(UserRegister.class)))
                    .thenThrow(new RegisterFailureException("Ce nom d'utilisateur est déjà pris"));

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "existinguser")
                            .param("email", "john@example.com")
                            .param("password", "password123")
                            .param("confirmPassword", "password123")
                            .param("terms", "true"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attribute("errorMessage", "Ce nom d'utilisateur est déjà pris"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenResourceNotFound() throws Exception {
            when(userRegistrationService.registerUser(any(UserRegister.class)))
                    .thenThrow(new RegisterFailureException("Resource not found"));

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "john@example.com")
                            .param("password", "password123")
                            .param("confirmPassword", "password123")
                            .param("terms", "true"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenGenericException() throws Exception {
            when(userRegistrationService.registerUser(any(UserRegister.class)))
                    .thenThrow(new RegisterFailureException("TEST"));

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "john@example.com")
                            .param("password", "password123")
                            .param("confirmPassword", "password123")
                            .param("terms", "true"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenPasswordsDontMatch() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "john@example.com")
                            .param("password", "password123")
                            .param("confirmPassword", "different456")
                            .param("terms", "true"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeHasFieldErrors("registerForm", "confirmPassword"));

            verifyNoInteractions(userRegistrationService);
        }


        @Test
        void forgotPassword_ShouldReturnForgotPasswordView() throws Exception {
            mockMvc.perform(get("/auth/forgot-password"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/forgot-password"));
        }



        @Test
        void forgotPasswordCode_ShouldReturnVerifyCodeView_WhenEmailExists() throws Exception {
            var user = new User(UUID.randomUUID(), "aziani", "Adel", "Ziani", "chef@forkeat.com", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);

            when(userService.findByEmail("chef@forkeat.com")).thenReturn(java.util.Optional.of(user));
            doNothing().when(emailVerificationService).sendPasswordChangeCode(user.id(), user.email());

            mockMvc.perform(post("/auth/forgot-password-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("email", "chef@forkeat.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/forgot-password-code"));
        }

        @Test
        void forgotPasswordCode_ShouldStillReturnCodeView_WhenEmailNotFound() throws Exception {
            when(userService.findByEmail("inconnu@forkeat.com"))
                    .thenReturn(java.util.Optional.empty());

            mockMvc.perform(post("/auth/forgot-password-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("email", "inconnu@forkeat.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/forgot-password-code"));
        }



        @Test
        void forgotPasswordVerifyCode_ShouldRedirectToLoginWithSuccess_WhenCodeIsValid() throws Exception {
            doNothing().when(userUpdateService)
                    .confirmForgotPasswordChange("chef@forkeat.com", "123456", "NewPassword1", "NewPassword1");

            mockMvc.perform(post("/auth/forgot-password-verify-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("email", "chef@forkeat.com")
                            .param("verificationCode", "123456")
                            .param("password", "NewPassword1")
                            .param("confirmPassword", "NewPassword1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/login"))
                    .andExpect(flash().attribute("success",
                            "Mot de passe réinitialisé avec succès ! Vous pouvez maintenant vous connecter."));
        }

        @Test
        void forgotPasswordVerifyCode_ShouldRedirectToForgotPassword_WhenCodeIsInvalid() throws Exception {
            doThrow(new VerificationException("Code incorrect ou expiré"))
                    .when(userUpdateService)
                    .confirmForgotPasswordChange("chef@forkeat.com", "000000", "NewPassword1", "NewPassword1");

            mockMvc.perform(post("/auth/forgot-password-verify-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("email", "chef@forkeat.com")
                            .param("verificationCode", "000000")
                            .param("password", "NewPassword1")
                            .param("confirmPassword", "NewPassword1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/forgot-password"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        void forgotPasswordVerifyCode_ShouldRedirectToForgotPassword_WhenPasswordsDontMatch() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("Les mots de passe ne correspondent pas"))
                    .when(userUpdateService)
                    .confirmForgotPasswordChange("chef@forkeat.com", "123456", "NewPassword1", "DifferentPass1");

            mockMvc.perform(post("/auth/forgot-password-verify-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("email", "chef@forkeat.com")
                            .param("verificationCode", "123456")
                            .param("password", "NewPassword1")
                            .param("confirmPassword", "DifferentPass1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/forgot-password"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        void forgotPasswordVerifyCode_ShouldRedirectToForgotPassword_WhenPasswordTooShort() throws Exception {
            doThrow(new CheckProfileUpdateFailureException("Le mot de passe doit contenir au moins 8 caractères"))
                    .when(userUpdateService)
                    .confirmForgotPasswordChange("chef@forkeat.com", "123456", "short", "short");

            mockMvc.perform(post("/auth/forgot-password-verify-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("email", "chef@forkeat.com")
                            .param("verificationCode", "123456")
                            .param("password", "short")
                            .param("confirmPassword", "short"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/forgot-password"))
                    .andExpect(flash().attributeExists("error"));

            verifyNoInteractions(emailVerificationService);
        }
    }
}