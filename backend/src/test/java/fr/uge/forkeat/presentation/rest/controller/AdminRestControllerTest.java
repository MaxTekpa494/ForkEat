package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.service.PlatformWalletService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.wallet.PlatformWallet;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.user.UserQueryService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private UserQueryService userQueryService;
    @MockitoBean
    private RecipeService recipeService;
    @MockitoBean
    private PlatformWalletService platformWalletService;
    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    AdminRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private User createModerator() {
        return new User(UUID.randomUUID(), "mod_user", "Mod", "User",
                "mod@forkeat.fr", UserRole.MODERATOR, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), true);
    }

    private User createAdmin() {
        return new User(UUID.randomUUID(), "admin_user", "Admin", "User",
                "admin@forkeat.fr", UserRole.ADMIN, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), true);
    }

    // ===== Register moderator =====

    @Nested
    class RegisterModeratorTests {

        @Test
        void shouldRegisterModeratorSuccessfully() throws Exception {
            var dto = new UserRegisterDTO("mod_user", "Mod", "User", "Password123", "mod@forkeat.fr");
            when(userRegistrationService.registerModerator(any())).thenReturn(createModerator());

            mockMvc.perform(post("/api/admin/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resource.username").value("mod_user"))
                    .andExpect(jsonPath("$.resource.role").value("MODERATOR"));

            verify(userRegistrationService).registerModerator(any());
            verify(userRegistrationService, never()).registerUser(any());
        }

        @Test
        void shouldReturnBadRequestWhenModeratorRegistrationFails() throws Exception {
            var dto = new UserRegisterDTO("mod_user", "Mod", "User", "Password123", "taken@forkeat.fr");
            when(userRegistrationService.registerModerator(any()))
                    .thenThrow(new RegisterFailureException("This email is already in use"));

            mockMvc.perform(post("/api/admin/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== Create admin =====

    @Nested
    class CreateAdminTests {

        @Test
        void shouldCreateAdminSuccessfully() throws Exception {
            var dto = new UserRegisterDTO("admin_user", "Admin", "User", "Password123", "admin@forkeat.fr");
            when(userRegistrationService.registerAdmin(any())).thenReturn(createAdmin());

            mockMvc.perform(post("/api/admin/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resource.username").value("admin_user"))
                    .andExpect(jsonPath("$.resource.role").value("ADMIN"));

            verify(userRegistrationService).registerAdmin(any());
        }
    }

    // ===== User lists =====

    @Nested
    class UserListTests {

        @Test
        void shouldReturnMemberList() throws Exception {
            var member = new User(UUID.randomUUID(), "member1", "First", "Last",
                    "m@forkeat.fr", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL,
                    Instant.now(), Instant.now(), true);
            when(userQueryService.getUsersByRole(UserRole.MEMBER)).thenReturn(List.of(member));

            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resources[0].username").value("member1"))
                    .andExpect(jsonPath("$.total").value(1));
        }

        @Test
        void shouldReturnModeratorList() throws Exception {
            when(userQueryService.getUsersByRole(UserRole.MODERATOR)).thenReturn(List.of(createModerator()));

            mockMvc.perform(get("/api/admin/moderators"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resources[0].username").value("mod_user"));
        }
    }

    // ===== Stats =====

    @Nested
    class StatsTests {

        @Test
        void shouldReturnUserStats() throws Exception {
            when(userQueryService.countByRole(UserRole.MEMBER)).thenReturn(42L);
            when(userQueryService.countByRole(UserRole.MODERATOR)).thenReturn(3L);
            when(userQueryService.countByRole(UserRole.ADMIN)).thenReturn(1L);

            mockMvc.perform(get("/api/admin/stats/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberCount").value(42))
                    .andExpect(jsonPath("$.moderatorCount").value(3))
                    .andExpect(jsonPath("$.adminCount").value(1));
        }

        @Test
        void shouldReturnRecipeStats() throws Exception {
            when(recipeService.countByStatus(RecipeStatus.PUBLISHED)).thenReturn(100L);
            when(recipeService.countByStatus(RecipeStatus.PENDING_REVIEW)).thenReturn(5L);
            when(recipeService.countByStatus(RecipeStatus.DRAFT)).thenReturn(20L);

            mockMvc.perform(get("/api/admin/stats/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.published").value(100))
                    .andExpect(jsonPath("$.pending").value(5))
                    .andExpect(jsonPath("$.draft").value(20));
        }
    }

    // ===== Platform wallets =====

    @Nested
    class PlatformWalletTests {

        @Test
        void shouldReturnBenefitsWallet() throws Exception {
            var wallet = new PlatformWallet(UUID.randomUUID(), PlatformWalletType.EARNINGS, 5000L, Instant.now());
            when(platformWalletService.getWallet(PlatformWalletType.EARNINGS)).thenReturn(wallet);

            mockMvc.perform(get("/api/admin/wallets/benefits"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("EARNINGS"))
                    .andExpect(jsonPath("$.balance").value(5000));
        }

        @Test
        void shouldReturnRedistributionWallet() throws Exception {
            var wallet = new PlatformWallet(UUID.randomUUID(), PlatformWalletType.REDISTRIBUTION, 1200L, Instant.now());
            when(platformWalletService.getWallet(PlatformWalletType.REDISTRIBUTION)).thenReturn(wallet);

            mockMvc.perform(get("/api/admin/wallets/redistribution"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("REDISTRIBUTION"))
                    .andExpect(jsonPath("$.balance").value(1200));
        }
    }
}
