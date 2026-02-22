package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.presentation.dto.user.UserProfileDTO;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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
    private JwtFilter jwtFilter;

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

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

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
}