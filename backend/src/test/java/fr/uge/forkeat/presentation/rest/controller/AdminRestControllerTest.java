package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminRestControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserRegistrationService userRegistrationService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    AdminRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldRegisterModeratorSuccessfully() throws Exception {
        var dto = new UserRegisterDTO("mod_user", "Mod", "User", "Password123", "mod@forkeat.fr");
        var moderator = new User(UUID.randomUUID(), "mod_user", "Mod", "User",
                "mod@forkeat.fr", UserRole.MODERATOR, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), false);

        when(userRegistrationService.registerModerator(any())).thenReturn(moderator);

        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.username").value("mod_user"))
                .andExpect(jsonPath("$.resource.role").value("MODERATOR"));

        verify(userRegistrationService).registerModerator(any());
    }

    @Test
    void shouldCallRegisterModeratorNotRegisterUser() throws Exception {
        var dto = new UserRegisterDTO("mod_user", "Mod", "User", "Password123", "mod@forkeat.fr");
        var moderator = new User(UUID.randomUUID(), "mod_user", "Mod", "User",
                "mod@forkeat.fr", UserRole.MODERATOR, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), false);

        when(userRegistrationService.registerModerator(any())).thenReturn(moderator);

        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userRegistrationService).registerModerator(any());
        verify(userRegistrationService, never()).registerUser(any());
    }
}
