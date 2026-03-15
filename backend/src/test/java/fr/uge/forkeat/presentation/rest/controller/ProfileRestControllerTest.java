package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.presentation.dto.user.ChangePasswordConfirmCodeDTO;
import fr.uge.forkeat.presentation.dto.user.ChangePasswordDTO;
import fr.uge.forkeat.presentation.dto.user.UserReportDTO;
import fr.uge.forkeat.presentation.dto.user.UserReportRequestDTO;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.UserReportService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.UserAlreadyReportedException;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.UserReportType;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.projection.PersonalizedUserProfile;
import fr.uge.forkeat.service.model.user.projection.UserAccountDetails;
import fr.uge.forkeat.service.model.user.projection.UserProfile;
import fr.uge.forkeat.service.model.user.projection.UserProfileWithRecipes;
import fr.uge.forkeat.service.model.user.projection.UserPublicProfile;
import fr.uge.forkeat.service.model.user.projection.UserSocialStats;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@WebMvcTest(ProfileRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileRestControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;
    @MockitoBean
    private AuthenticationPort authenticationPort;
    @MockitoBean
    private JwtUtils jwtUtils; // nécessaire pour SecurityConfig (évite @Value JWT_SECRET manquant)
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserReportService userReportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ProfileRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        when(authenticationPort.extractUsername()).thenReturn("viewer");
    }

    private PersonalizedUserProfile buildProfile(String username, boolean followed) {
        var publicProfile = new UserPublicProfile(username, "Jean", "Dupont");
        var socialStats = new UserSocialStats(10, 5, 20, 3);
        var profile = new UserProfile(publicProfile, socialStats);
        var recipes = new PageResult<PersonalizedRecipeSummary>(List.of(), 0L);
        var profileWithRecipes = new UserProfileWithRecipes(profile, recipes);
        return new PersonalizedUserProfile(profileWithRecipes, followed);
    }

    @Nested
    class GetProfileTests {

        @Test
        void shouldReturnProfile_WhenUserExists() throws Exception {
            when(profileService.getProfileInfos(eq("chef"), anyInt(), anyInt(), eq("viewer")))
                    .thenReturn(buildProfile("chef", false));

            mockMvc.perform(get("/api/profile/chef"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.profile.publicProfile.username").value("chef"))
                    .andExpect(jsonPath("$.resource.profile.publicProfile.firstName").value("Jean"))
                    .andExpect(jsonPath("$.resource.totalRecipes").value(0))
                    .andExpect(jsonPath("$.resource.currentPage").value(0))
                    .andExpect(jsonPath("$.resource.followedByCurrentUser").value(false));

            verify(profileService).getProfileInfos("chef", 0, 12, "viewer");
        }

        @Test
        void shouldReturnFollowedTrue_WhenCurrentUserFollows() throws Exception {
            when(profileService.getProfileInfos(eq("chef"), anyInt(), anyInt(), eq("viewer")))
                    .thenReturn(buildProfile("chef", true));

            mockMvc.perform(get("/api/profile/chef"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.followedByCurrentUser").value(true));
        }

        @Test
        void shouldUseDefaultPagination() throws Exception {
            when(profileService.getProfileInfos(any(), anyInt(), anyInt(), any()))
                    .thenReturn(buildProfile("chef", false));

            mockMvc.perform(get("/api/profile/chef"))
                    .andExpect(status().isOk());

            verify(profileService).getProfileInfos("chef", 0, 12, "viewer");
        }

        @Test
        void shouldForwardPaginationParams() throws Exception {
            when(profileService.getProfileInfos(eq("chef"), eq(2), eq(5), eq("viewer")))
                    .thenReturn(buildProfile("chef", false));

            mockMvc.perform(get("/api/profile/chef")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.currentPage").value(2));

            verify(profileService).getProfileInfos("chef", 2, 5, "viewer");
        }

        @Test
        void shouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
            when(profileService.getProfileInfos(eq("unknown"), anyInt(), anyInt(), any()))
                    .thenThrow(new ResourceNotFoundException("User not found: unknown"));

            mockMvc.perform(get("/api/profile/unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("User not found: unknown"));
        }

        @Test
        void shouldExtractCurrentUsernameFromAuth() throws Exception {
            when(profileService.getProfileInfos(any(), anyInt(), anyInt(), any()))
                    .thenReturn(buildProfile("chef", false));

            mockMvc.perform(get("/api/profile/chef"))
                    .andExpect(status().isOk());

            verify(authenticationPort).extractUsername();
        }
    }

    @Nested
    class GetMyProfileTests {

        @Test
        void shouldReturnDashboard_WhenAuthenticated() throws Exception {
            when(profileService.getAccountDetails("viewer")).thenReturn(buildAccountDetails("viewer"));

            mockMvc.perform(get("/api/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.user.username").value("viewer"))
                    .andExpect(jsonPath("$.resource.followerCount").value(10))
                    .andExpect(jsonPath("$.resource.walletBalance").value(1000))
                    .andExpect(jsonPath("$.resource.recipeCount").value(5));
        }

        @Test
        void shouldCallGetAccountDetailsWithCurrentUsername() throws Exception {
            when(profileService.getAccountDetails("viewer")).thenReturn(buildAccountDetails("viewer"));

            mockMvc.perform(get("/api/profile"))
                    .andExpect(status().isOk());

            verify(profileService).getAccountDetails("viewer");
        }

        @Test
        void shouldReturn404_WhenUserNotFound() throws Exception {
            when(profileService.getAccountDetails(any()))
                    .thenThrow(new ResourceNotFoundException("User not found: viewer"));

            mockMvc.perform(get("/api/profile"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found: viewer"));
        }
    }

    @Nested
    class FollowTests {

        @Test
        void follow_ShouldReturn200_WhenSuccessful() throws Exception {
            doNothing().when(userService).follow("viewer", "chef");

            mockMvc.perform(put("/api/profile/chef/follow"))
                    .andExpect(status().isOk());

            verify(userService).follow("viewer", "chef");
        }

        @Test
        void follow_ShouldReturn404_WhenUserNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found: unknown"))
                    .when(userService).follow("viewer", "unknown");

            mockMvc.perform(put("/api/profile/unknown/follow"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void unfollow_ShouldReturn200_WhenSuccessful() throws Exception {
            doNothing().when(userService).unfollow("viewer", "chef");

            mockMvc.perform(delete("/api/profile/chef/follow"))
                    .andExpect(status().isOk());

            verify(userService).unfollow("viewer", "chef");
        }

        @Test
        void unfollow_ShouldReturn404_WhenUserNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found: unknown"))
                    .when(userService).unfollow("viewer", "unknown");

            mockMvc.perform(delete("/api/profile/unknown/follow"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ReportUserTests {

        @Test
        void shouldReturn201_WhenReportCreatedSuccessfully() throws Exception {
            var reportedUserId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var request = new UserReportRequestDTO(UserReportType.SPAM, "Ceci est du spam");
            var report = new UserReport(
                    UUID.randomUUID(), reportedUserId, reporterId,
                    UserReportType.SPAM, ReportStatus.PENDING,
                    "Ceci est du spam", null, null, null, null
            );

            when(authenticationPort.extractUsername()).thenReturn("reporter");
            when(userReportService.reportUser(any())).thenReturn(report);

            mockMvc.perform(post("/api/profile/otheruser/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resource.reportedUserId").value(reportedUserId.toString()))
                    .andExpect(jsonPath("$.resource.status").value("PENDING"))
                    .andExpect(jsonPath("$.resource.reportType").value("SPAM"));

            verify(userReportService).reportUser(any());
        }

        @Test
        void shouldReturn409_WhenAlreadyReported() throws Exception {
            var request = new UserReportRequestDTO(UserReportType.HARASSMENT, "Justification");

            when(authenticationPort.extractUsername()).thenReturn("reporter");
            when(userReportService.reportUser(any()))
                    .thenThrow(new UserAlreadyReportedException(UUID.randomUUID(), UUID.randomUUID()));

            mockMvc.perform(post("/api/profile/otheruser/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Conflict"));
        }

        @Test
        void shouldReturn404_WhenUserDoesNotExist() throws Exception {
            var request = new UserReportRequestDTO(UserReportType.SPAM, "Justification");

            when(authenticationPort.extractUsername()).thenReturn("reporter");
            when(userReportService.reportUser(any()))
                    .thenThrow(new ResourceNotFoundException("User not found: otheruser"));

            mockMvc.perform(post("/api/profile/otheruser/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }

    private UserAccountDetails buildAccountDetails(String username) {
        var user = new User(UUID.randomUUID(), username, "Jean", "Dupont", "jean@example.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
        var socialStats = new UserSocialStats(10, 5, 20, 3);
        return new UserAccountDetails(user, socialStats, 1000L, 5L);
    }
}