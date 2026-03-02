package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.presentation.dto.user.ChangePasswordConfirmCodeDTO;
import fr.uge.forkeat.presentation.dto.user.ChangePasswordDTO;
import fr.uge.forkeat.presentation.dto.user.UserLoginDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.GoogleTokenVerificationService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserUpdateService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthRestControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserRegistrationService userRegistrationService;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtUtils jwtUtils; // nécessaire pour SecurityConfig (évite @Value JWT_SECRET manquant)
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private EmailVerificationService emailVerificationService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private GoogleTokenVerificationService googleTokenVerificationService;
    @MockitoBean
    private AuthenticationPort authPort;
    @MockitoBean
    private UserUpdateService userUpdateService;

    @Autowired
    AuthRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private User createUser() {
        return new User(UUID.randomUUID(), "testuser", "Test", "User",
                "test@forkeat.fr", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), false);
    }

    @Nested
    class RegisterTests {

        @Test
        void shouldRegisterSuccessfully() throws Exception {
            var dto = new UserRegisterDTO("testuser", "Test", "User", "Password123", "test@forkeat.fr");
            var user = createUser();

            when(userRegistrationService.registerUser(any())).thenReturn(user);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.username").value("testuser"))
                    .andExpect(jsonPath("$.resource.email").value("test@forkeat.fr"))
                    .andExpect(jsonPath("$.resource.role").value("MEMBER"));

            verify(userRegistrationService).registerUser(any());
        }

        @Test
        void shouldReturnBadRequest_WhenPasswordTooShort() throws Exception {
            var dto = new UserRegisterDTO("testuser", "Test", "User", "short", "test@forkeat.fr");

            when(userRegistrationService.registerUser(any()))
                    .thenThrow(new RegisterFailureException("Le mot de passe doit contenir au moins 8 caractères"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));

            verify(userRegistrationService).registerUser(any());
        }

        @Test
        void shouldReturnBadRequest_WhenEmailAlreadyTaken() throws Exception {
            var dto = new UserRegisterDTO("testuser", "Test", "User", "Password123", "taken@forkeat.fr");

            when(userRegistrationService.registerUser(any()))
                    .thenThrow(new RegisterFailureException("Email already exists"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }
    }

    @Nested
    class LoginTests {

        @Test
        void shouldLoginSuccessfully() throws Exception {
            var dto = new UserLoginDTO("testuser", "Password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken("testuser", null));
            when(authPort.generateToken("testuser")).thenReturn("jwt-token-value");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-value"))
                    .andExpect(jsonPath("$.type").value("Bearer"));

            verify(authenticationManager).authenticate(any());
            verify(authPort).generateToken("testuser");
        }

        @Test
        void shouldReturnUnauthorized_WhenBadCredentials() throws Exception {
            var dto = new UserLoginDTO("testuser", "WrongPassword");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class ForgotPasswordTests {

        @Test
        void shouldSendResetCode_WhenEmailExists() throws Exception {
            var dto = new ChangePasswordDTO("test@forkeat.fr");
            var user = createUser();

            when(userService.findByEmail("test@forkeat.fr")).thenReturn(java.util.Optional.of(user));
            doNothing().when(emailVerificationService).sendPasswordChangeCode(any(), any());

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(emailVerificationService).sendPasswordChangeCode(any(), any());
        }

        @Test
        void shouldReturnOk_WhenEmailDoesNotExist() throws Exception {
            var dto = new ChangePasswordDTO("unknown@forkeat.fr");

            when(userService.findByEmail("unknown@forkeat.fr")).thenReturn(java.util.Optional.empty());

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verifyNoInteractions(emailVerificationService);
        }
    }

    @Nested
    class ForgotPasswordConfirmCodeTests {

        @Test
        void shouldConfirmCode_WhenValidRequest() throws Exception {
            var dto = new ChangePasswordConfirmCodeDTO("test@forkeat.fr", "123456", "NewPassword1", "NewPassword1");
            doNothing().when(userUpdateService).confirmForgotPasswordChange(any(), any(), any(), any());

            mockMvc.perform(post("/api/auth/forgot-password/confirm-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(userUpdateService).confirmForgotPasswordChange("test@forkeat.fr", "123456", "NewPassword1", "NewPassword1");
        }

        @Test
        void shouldReturnBadRequest_WhenCodeIsInvalid() throws Exception {
            var dto = new ChangePasswordConfirmCodeDTO("test@forkeat.fr", "000000", "NewPassword1", "NewPassword1");
            doThrow(new VerificationException("Code incorrect"))
                    .when(userUpdateService).confirmForgotPasswordChange(any(), any(), any(), any());

            mockMvc.perform(post("/api/auth/forgot-password/confirm-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Code incorrect"));
        }

        @Test
        void shouldReturnNotFound_WhenEmailDoesNotExist() throws Exception {
            var dto = new ChangePasswordConfirmCodeDTO("unknown@forkeat.fr", "123456", "NewPassword1", "NewPassword1");
            doThrow(new ResourceNotFoundException("User not found"))
                    .when(userUpdateService).confirmForgotPasswordChange(any(), any(), any(), any());

            mockMvc.perform(post("/api/auth/forgot-password/confirm-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }
}
