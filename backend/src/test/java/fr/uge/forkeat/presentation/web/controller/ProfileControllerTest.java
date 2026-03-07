package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.presentation.dto.user.UserProfileDTO;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.UserReportService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.UserAlreadyReportedException;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileWebController.class)
class ProfileControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserReportService userReportService;

    private User testUser;

    @Autowired
    public ProfileControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() throws Exception {
        testUser = new User(
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

        when(authPort.extractUsername()).thenReturn(testUser.username());
    }

    @Nested
    class ProfilePageTests {

        private UserAccountDetails buildAccountDetails(long balance) {
            var socialStats = new UserSocialStats(10, 5, 20, 3);
            return new UserAccountDetails(testUser, socialStats, balance, 4L);
        }

        @Test
        @WithMockUser(username = "testuser")
        void profile_ShouldReturnProfileView_WhenAuthenticated() throws Exception {
            var vm = buildAccountDetails(1000L);
            when(profileService.getAccountDetails("testuser")).thenReturn(vm);

            mockMvc.perform(get("/profile"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("profile/index"))
                    .andExpect(model().attributeExists("vm"))
                    .andExpect(model().attribute("vm", vm));

            verify(profileService).getAccountDetails("testuser");
        }

        @Test
        @WithMockUser(username = "testuser")
        void profile_ShouldReturn404_WhenUserNotFound() throws Exception {
            when(profileService.getAccountDetails("testuser"))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            mockMvc.perform(get("/profile"))
                    .andExpect(status().is4xxClientError());

            verify(profileService).getAccountDetails("testuser");
        }

        @Test
        @WithMockUser(username = "testuser")
        void profile_ShouldExtractUsernameFromAuth() throws Exception {
            when(profileService.getAccountDetails("testuser")).thenReturn(buildAccountDetails(500L));

            mockMvc.perform(get("/profile"))
                    .andExpect(status().isOk());

            verify(authPort).extractUsername();
            verify(profileService).getAccountDetails("testuser");
        }
    }

    @Nested
    class UserProfilePageTests {

        private PersonalizedUserProfile buildPersonalizedProfile(boolean followed) {
            var publicProfile = new UserPublicProfile("otheruser", "John", "Doe");
            var socialStats = new UserSocialStats(10, 5, 20, 3);
            var profile = new UserProfile(publicProfile, socialStats);
            var recipes = new PageResult<PersonalizedRecipeSummary>(List.of(), 0L);
            var profileWithRecipes = new UserProfileWithRecipes(profile, recipes);
            return new PersonalizedUserProfile(profileWithRecipes, followed);
        }

        @Test
        @WithMockUser(username = "testuser")
        void userProfile_ShouldRedirectToOwnProfile_WhenUsernameMatchesCurrent() throws Exception {
            mockMvc.perform(get("/profile/testuser"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile"));

            verify(profileService, never()).getProfileInfos(any(), anyInt(), anyInt(), any());
        }

        @Test
        @WithMockUser(username = "testuser")
        void userProfile_ShouldReturnUserView_WhenViewingOtherProfile() throws Exception {
            when(profileService.getProfileInfos(eq("otheruser"), anyInt(), anyInt(), eq("testuser")))
                    .thenReturn(buildPersonalizedProfile(false));

            mockMvc.perform(get("/profile/otheruser"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("profile/user"))
                    .andExpect(model().attributeExists("vm"));

            verify(profileService).getProfileInfos(eq("otheruser"), anyInt(), anyInt(), eq("testuser"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void userProfile_ShouldExposeFollowedState_WhenFollowing() throws Exception {
            when(profileService.getProfileInfos(eq("otheruser"), anyInt(), anyInt(), eq("testuser")))
                    .thenReturn(buildPersonalizedProfile(true));

            mockMvc.perform(get("/profile/otheruser"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        var vm = (UserProfileDTO) result.getModelAndView().getModel().get("vm");
                        assertTrue(vm.followedByCurrentUser());
                    });
        }
    }

    @Nested
    class FollowTests {

        @Test
        @WithMockUser(username = "testuser")
        void follow_ShouldRedirectToProfile_WhenSuccessful() throws Exception {
            doNothing().when(userService).follow("testuser", "otheruser");

            mockMvc.perform(post("/profile/otheruser/follow").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile/otheruser"));

            verify(userService).follow("testuser", "otheruser");
        }

        @Test
        @WithMockUser(username = "testuser")
        void follow_ShouldReturn4xx_WhenUserNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found: unknown"))
                    .when(userService).follow("testuser", "unknown");

            mockMvc.perform(post("/profile/unknown/follow").with(csrf()))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockUser(username = "testuser")
        void unfollow_ShouldRedirectToProfile_WhenSuccessful() throws Exception {
            doNothing().when(userService).unfollow("testuser", "otheruser");

            mockMvc.perform(post("/profile/otheruser/unfollow").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile/otheruser"));

            verify(userService).unfollow("testuser", "otheruser");
        }

        @Test
        @WithMockUser(username = "testuser")
        void unfollow_ShouldReturn4xx_WhenUserNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found: unknown"))
                    .when(userService).unfollow("testuser", "unknown");

            mockMvc.perform(post("/profile/unknown/unfollow").with(csrf()))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    class ReportUserTests {

        @Test
        @WithMockUser(username = "testuser")
        void report_ShouldRedirectWithSuccessFlash_WhenSuccessful() throws Exception {
            when(userReportService.reportUser(any())).thenReturn(null);

            mockMvc.perform(post("/profile/otheruser/report").with(csrf())
                            .param("reportType", "SPAM")
                            .param("justification", "Ceci est du spam"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile/otheruser"))
                    .andExpect(flash().attributeExists("reportSuccess"));

            verify(userReportService).reportUser(any());
        }

        @Test
        @WithMockUser(username = "testuser")
        void report_ShouldRedirectWithErrorFlash_WhenAlreadyReported() throws Exception {
            when(userReportService.reportUser(any()))
                    .thenThrow(new UserAlreadyReportedException(UUID.randomUUID(), UUID.randomUUID()));

            mockMvc.perform(post("/profile/otheruser/report").with(csrf())
                            .param("reportType", "HARASSMENT")
                            .param("justification", "Déjà signalé"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("reportError"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void report_ShouldReturn4xx_WhenUserNotFound() throws Exception {
            when(userReportService.reportUser(any()))
                    .thenThrow(new ResourceNotFoundException("User not found: otheruser"));

            mockMvc.perform(post("/profile/otheruser/report").with(csrf())
                            .param("reportType", "SPAM")
                            .param("justification", "Utilisateur introuvable"))
                    .andExpect(status().is4xxClientError());
        }
    }
}