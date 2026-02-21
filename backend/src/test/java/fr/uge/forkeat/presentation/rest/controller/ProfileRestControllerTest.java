package fr.uge.forkeat.presentation.rest.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.presentation.dto.user.ChangePasswordConfirmCodeDTO;
import fr.uge.forkeat.presentation.dto.user.ChangePasswordDTO;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.VerificationException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileRestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProfileRestControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    ProfileRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }


    private User createUser() {
        return new User(UUID.randomUUID(), "testuser", "Test", "User",
                "test@forkeat.fr", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), false);
    }

    private ChangePasswordDTO createChangePasswordDTO() {
        return new ChangePasswordDTO("test@forkeat.fr");
    }

    private ChangePasswordConfirmCodeDTO createChangePasswordConfirmCodeDTO() {
        return new ChangePasswordConfirmCodeDTO("test@forkeat.fr", "000000", "Password1234", "Password1234");
    }

    @Nested
    class ForgotPasswordTests{


        @Test
        public void AskingForgotPasswordSuccessfull() throws Exception {
            var user = createUser();

            when(userQueryService.getUserByEmail(any())).thenReturn(user);
            doNothing().when(emailVerificationService).sendPasswordChangeCode(any(), any());


            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createChangePasswordDTO())))
                    .andExpect(status().isOk());
        }

        @Test
        public void AskingForgotPasswordShouldReturnAnErrorWhenEmailNotExist() throws Exception {
            var user = createUser();

            when(userQueryService.getUserByEmail(any())).thenThrow(new ResourceNotFoundException("User not found with email: test@forkeat.fr"));
            doNothing().when(emailVerificationService).sendPasswordChangeCode(any(), any());


            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createChangePasswordDTO())))
                    .andExpect(status().isOk());
        }

    }

    @Nested
    class ForgotPasswordCodeTests{

        @Test
        public void ForgotPasswordCodeShouldReturnAnErrorWhenCodeIsWrong() throws Exception {
            var user = createUser();

            when(userQueryService.getUserByEmail(any())).thenReturn(user);
            doThrow(new VerificationException("Code incorrect")).when(emailVerificationService).confirmPasswordChange(any(), any(), any());


            mockMvc.perform(post("/api/auth/forgot-password/confirm-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createChangePasswordConfirmCodeDTO())))
                    .andExpect(status().isBadRequest());
        }


        @Test
        public void ForgotPasswordCodeShouldReturnAnErrorWhenCodeIsExpired() throws Exception {
            var user = createUser();

            when(userQueryService.getUserByEmail(any())).thenReturn(user);
            doThrow(new VerificationException("Le code a expiré. Veuillez recommencer.")).when(emailVerificationService).confirmPasswordChange(any(), any(), any());


            mockMvc.perform(post("/api/auth/forgot-password/confirm-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createChangePasswordConfirmCodeDTO())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void ForgotPasswordCodeShouldReturnAnErrorWhenEmailIsNotAskingAResetPassword() throws Exception {
            var user = createUser();

            when(userQueryService.getUserByEmail(any())).thenReturn(user);
            doThrow(new VerificationException("Aucun changement de mot de passe en attente")).when(emailVerificationService).confirmPasswordChange(any(), any(), any());


            mockMvc.perform(post("/api/auth/forgot-password/confirm-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createChangePasswordConfirmCodeDTO())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void ForgotPasswordCodeSuccessfull() throws Exception {
            var user = createUser();

            when(userQueryService.getUserByEmail(any())).thenReturn(user);
            doNothing().when(emailVerificationService).confirmPasswordChange(any(), any(), any());


            mockMvc.perform(post("/api/auth/forgot-password/confirm-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createChangePasswordConfirmCodeDTO())))
                    .andExpect(status().isOk());
        }
    }
}
