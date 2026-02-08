package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.exception.RegisterFailure;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

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
                Instant.now()
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
            when(userRegistrationService.registerUser(
                   any(UserRegister.class)
            )).thenReturn(newUser);

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
                    .andExpect(redirectedUrl("/auth/login"))
                    .andExpect(flash().attributeExists("success"));

            verify(userRegistrationService).registerUser(any(UserRegister.class));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenEmailAlreadyExists() throws Exception {
            // Given
            when(userRegistrationService.registerUser(any(UserRegister.class)))
                    .thenThrow(new RegisterFailure("Cet email est déjà utilisé"));

            // When/Then
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
            // Given
            when(userRegistrationService.registerUser(any(UserRegister.class)))
                    .thenThrow(new RegisterFailure("Ce nom d'utilisateur est déjà pris"));

            // When/Then
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
            // Given
            when(userRegistrationService.registerUser(any(UserRegister.class)))
                    .thenThrow(new RegisterFailure("Resource not found"));

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
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenGenericException() throws Exception {
            // Given
            when(userRegistrationService.registerUser(any(UserRegister.class)))
                    .thenThrow(new RegisterFailure("TEST"));

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
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenPasswordsDontMatch() throws Exception {
            // When/Then
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
    }
}
