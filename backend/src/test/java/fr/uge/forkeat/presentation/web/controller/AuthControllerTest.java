package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.service.CustomUserDetailsService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.*;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User createTestUser() {
        return new User(
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
    }

    @Nested
    class LoginPageTests {

        @Test
        void loginPage_ShouldReturnLoginView_WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/login"));
        }
    }

    @Nested
    class RegisterPageTests {

        @Test
        void registerPage_ShouldReturnRegisterView_WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/register"))
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
                    anyString(), anyString(), anyString(), anyString(), anyString(), eq(UserRole.MEMBER)
            )).thenReturn(newUser);

            // When/Then
            mockMvc.perform(post("/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "john@example.com")
                            .param("password", "password123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"))
                    .andExpect(flash().attributeExists("success"));

            verify(userRegistrationService).registerUser("John", "Doe", "johndoe", "john@example.com", "password123", UserRole.MEMBER);
        }

        @Test
        void register_ShouldReturnRegisterView_WhenEmailAlreadyExists() throws Exception {
            // Given
            when(userRegistrationService.registerUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), eq(UserRole.MEMBER)
            )).thenThrow(new IllegalArgumentException("Cet email est déjà utilisé"));

            // When/Then
            mockMvc.perform(post("/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "existing@example.com")
                            .param("password", "password123"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenUsernameAlreadyExists() throws Exception {
            // Given
            when(userRegistrationService.registerUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), eq(UserRole.MEMBER)
            )).thenThrow(new IllegalArgumentException("Ce nom d'utilisateur est déjà pris"));

            // When/Then
            mockMvc.perform(post("/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "existinguser")
                            .param("email", "john@example.com")
                            .param("password", "password123"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attribute("error", "Ce nom d'utilisateur est déjà pris"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenResourceNotFound() throws Exception {
            // Given
            when(userRegistrationService.registerUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), eq(UserRole.MEMBER)
            )).thenThrow(new ResourceNotFoundException("Resource not found"));

            // When/Then
            mockMvc.perform(post("/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "john@example.com")
                            .param("password", "password123"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        void register_ShouldReturnRegisterView_WhenGenericException() throws Exception {
            // Given
            when(userRegistrationService.registerUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), eq(UserRole.MEMBER)
            )).thenThrow(new RuntimeException("Unexpected error"));

            // When/Then
            mockMvc.perform(post("/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("firstName", "John")
                            .param("lastName", "Doe")
                            .param("userName", "johndoe")
                            .param("email", "john@example.com")
                            .param("password", "password123"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/register"))
                    .andExpect(model().attributeExists("error"));
        }
    }
}
