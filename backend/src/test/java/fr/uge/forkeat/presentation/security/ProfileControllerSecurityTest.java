package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.projection.*;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;



@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Transactional
public class ProfileControllerSecurityTest extends AbstractIntegrationTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private AuthenticationPort authenticationPort;

    @BeforeEach
    void setup() {

        // Auth mock — commun à tous les endpoints
        when(authenticationPort.extractUsername()).thenReturn("PaxGPT");

        // Mock AccountDetails pour /api/profile
        var mockAccountDetails = new UserAccountDetails(createUser(UUID.randomUUID()), new UserSocialStats(0L,0L,0L,0L), 1L, 1L);
        when(profileService.getAccountDetails(any()))
                .thenReturn(mockAccountDetails);


        when(profileService.getProfileInfos(any(), anyInt(), anyInt(), any()))
                .thenReturn(buildProfile("pax", false));
    }

    @Nested
    class ProfileRestController{


        @Test
        void testGetMyProfile() throws Exception {
            testRights(get("/api/profile"), AuthorizationTest.MEMBER);
        }

        @Test
        void testGetProfile() throws Exception {
            testRights(get("/api/profile/{username}", "MaximusPrime"), AuthorizationTest.MEMBER);
        }

        @Test
        void testGetProfileWithPagination() throws Exception {
            testRights(
                    get("/api/profile/{username}", "pax")
                            .param("page", "1")
                            .param("size", "6"),
                    AuthorizationTest.MEMBER
            );
        }
    }
    @Nested
    class ProfileWebControllerSecurityTest {

        @Test
        void testProfile() throws Exception {
            testRightsMVCNoRedirect(get("/profile"), AuthorizationTest.MEMBER);
        }

        @Test
        void testUserProfile() throws Exception {
            testRightsMVCNoRedirect(get("/profile/{username}", "MaximusPrime"), AuthorizationTest.MEMBER);
        }

        @Test
        void testUserProfileWithPagination() throws Exception {
            testRightsMVCNoRedirect(get("/profile/{username}", "MaximusPrime")
                    .param("page", "0")
                    .param("size", "12"), AuthorizationTest.MEMBER);
        }
    }


    private void testRights(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expected = status().isForbidden();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().is2xxSuccessful() : status().isUnauthorized();
        mockMvc.perform(requestBuilders).andExpect(expectedForUnauthenticated);

        if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


        if(authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }

    private User createUser(UUID id) {
        return new User(id, "PaxGPT", "Pax", "Pekpa", "a@gmail.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }

    private UserAccountDetails buildAccountDetails(String username) {
        var user = new User(UUID.randomUUID(), username, "Jean", "Dupont", "jean@example.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
        var socialStats = new UserSocialStats(10, 5, 20, 3);
        return new UserAccountDetails(user, socialStats, 1000L, 5L);
    }

    private PersonalizedUserProfile buildProfile(String username, boolean followed) {
        var publicProfile = new UserPublicProfile(username, "Jean", "Dupont");
        var socialStats = new UserSocialStats(10, 5, 20, 3);
        var profile = new UserProfile(publicProfile, socialStats);
        var recipes = new PageResult<PersonalizedRecipeSummary>(List.of(), 0L);
        var profileWithRecipes = new UserProfileWithRecipes(profile, recipes);
        return new PersonalizedUserProfile(profileWithRecipes, followed);
    }


    private void testRightsMVCNoRedirect(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expected = status().is3xxRedirection();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().is2xxSuccessful() : status().is3xxRedirection();
        mockMvc.perform(requestBuilders).andExpect(expectedForUnauthenticated);

        if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().is2xxSuccessful();
        }

        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


        if(authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }
}
