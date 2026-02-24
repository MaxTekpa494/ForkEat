package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.user.projection.PersonalizedUserProfile;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private CustomUserDetailsService customUserDetailsService;

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

            verify(profileService).getProfileInfos("chef", 0, 10, "viewer");
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

            verify(profileService).getProfileInfos("chef", 0, 10, "viewer");
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
}