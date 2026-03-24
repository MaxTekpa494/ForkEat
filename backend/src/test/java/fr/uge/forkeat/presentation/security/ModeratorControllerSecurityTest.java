package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.service.*;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.recipe.projection.RecipeReportDetails;
import fr.uge.forkeat.service.model.user.UserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import fr.uge.forkeat.service.model.user.projection.UserReportDetails;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Transactional
public class ModeratorControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private RecipeModerationActionService recipeModerationActionService;

    @MockitoBean
    private RecipeReportService recipeReportService;

    @MockitoBean
    private UserReportService userReportService;

    @MockitoBean
    private UserModerationActionService userModerationActionService;

    private UUID recipeId;
    private UUID reportId;
    private UUID userId;

    @BeforeEach
    void setup() {
        recipeId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        userId = UUID.randomUUID();

        when(authPort.extractUsername()).thenReturn("PAX");

        when(recipeService.getRecipesToModerate(anyString(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(), 0));

        when(recipeModerationActionService.moderateRecipe(any())).thenReturn(new RecipeModerationAction(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), RecipeModerationActionType.APPROVED, "a", Instant.now(), Instant.now(), UUID.randomUUID()));

        when(recipeReportService.getReportsToModerate(anyString(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.<RecipeReportDetails>of(), 0));

        when(userReportService.getReportsToModerate(anyString(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.<UserReportDetails>of(), 0));

        when(userModerationActionService.moderateUser(any())).thenReturn(new UserModerationAction(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UserModerationActionType.BANNED, "a", Instant.now(), Instant.now(), Instant.now(), UUID.randomUUID()));
    }

    @Nested
    class ModeratorRestControllerSecurityTests {

        @Test
        void testGetPendingRecipes() throws Exception {
            testRights(get("/api/moderator/recipes/pending")
                    .param("size", "10")
                    .param("page", "0"), AuthorizationTest.MODERATOR);
        }

        @Test
        void testValidateRecipe() throws Exception {
            testRights(post("/api/moderator/recipes/" + recipeId + "/validate"),
                    AuthorizationTest.MODERATOR);
        }

        @Test
        void testRejectRecipe() throws Exception {
            testRights(post("/api/moderator/recipes/" + recipeId + "/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "justification": "Content violates community guidelines"
                        }
                    """), AuthorizationTest.MODERATOR);
        }

        @Test
        void testGetReportedRecipes() throws Exception {
            testRights(get("/api/moderator/recipes/reports")
                    .param("size", "10")
                    .param("page", "0"), AuthorizationTest.MODERATOR);
        }

        @Test
        void testGetReportedUsers() throws Exception {
            testRights(get("/api/moderator/users/reports")
                    .param("size", "10")
                    .param("page", "0"), AuthorizationTest.MODERATOR);
        }

        @Test
        void testValidateReport() throws Exception {
            testRights(post("/api/moderator/recipes/reports/" + reportId + "/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "recipeId": "%s"
                        }
                    """.formatted(recipeId)), AuthorizationTest.MODERATOR);
        }

        @Test
        void testDismissReport() throws Exception {
            testRights(post("/api/moderator/recipes/reports/" + reportId + "/dismiss")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "recipeId": "%s",
                            "justification": "Report is not valid"
                        }
                    """.formatted(recipeId)), AuthorizationTest.MODERATOR);
        }

        @Test
        void testResolveUserReport() throws Exception {
            testRights(post("/api/moderator/users/reports/" + reportId + "/resolve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "userId": "%s",
                            "action": "WARNING",
                            "justification": "Inappropriate behavior",
                            "suspensionDays": 0,
                            "suspensionHours": 0
                        }
                    """.formatted(userId)), AuthorizationTest.MODERATOR);
        }
    }

    @Nested
    class ModeratorWebControllerTests {

        @Test
        void testPendingRecipes() throws Exception {
            testRightsMVCNoRedirect(get("/moderator/recipes"), AuthorizationTest.MODERATOR);
        }

        @Test
        void testValidateRecipe() throws Exception {
            testRightsMVC(post("/moderator/recipes/{id}/validate", UUID.randomUUID()),
                    AuthorizationTest.MODERATOR);
        }

        @Test
        void testRejectRecipe() throws Exception {
            testRightsMVC(post("/moderator/recipes/{id}/reject", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("justification", "inappropriate content"),
                    AuthorizationTest.MODERATOR);
        }

        @Test
        void testReportedRecipes() throws Exception {
            testRightsMVCNoRedirect(get("/moderator/recipes/reports"), AuthorizationTest.MODERATOR);
        }

        @Test
        void testReportedUsers() throws Exception {
            testRightsMVCNoRedirect(get("/moderator/users/reports"), AuthorizationTest.MODERATOR);
        }

        @Test
        void testValidateRecipeReport() throws Exception {
            testRightsMVC(post("/moderator/recipes/reports/{reportId}/validate", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("recipeId", UUID.randomUUID().toString()),
                    AuthorizationTest.MODERATOR);
        }

        @Test
        void testDismissRecipeReport() throws Exception {
            testRightsMVC(post("/moderator/recipes/reports/{reportId}/dismiss", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("recipeId", UUID.randomUUID().toString())
                            .param("justification", "not a violation"),
                    AuthorizationTest.MODERATOR);
        }

        @Test
        void testResolveUserReport() throws Exception {
            testRightsMVC(post("/moderator/users/reports/{reportId}/resolve", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("userId", UUID.randomUUID().toString())
                            .param("action", "WARNING")
                            .param("justification", "first warning")
                            .param("suspensionDays", "0")
                            .param("suspensionHours", "0"),
                    AuthorizationTest.MODERATOR);
        }
    }

    private void testRights(MockHttpServletRequestBuilder requestBuilder, AuthorizationTest authorization) throws Exception {
        var expected = status().isForbidden();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED)
                ? status().is2xxSuccessful()
                : status().isUnauthorized();
        mockMvc.perform(requestBuilder).andExpect(expectedForUnauthenticated);

        if (authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilder.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilder.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilder.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilder.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }

    private void testRightsMVCNoRedirect(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expected = status().is3xxRedirection();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().is2xxSuccessful() : status().is3xxRedirection();
        mockMvc.perform(requestBuilders).andExpect(expectedForUnauthenticated);

        if (authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }

    private void testRightsMVC(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expectedBlocked = status().is3xxRedirection();
        var expectedSuccess = status().is3xxRedirection(); // POST redirige vers /account en cas de succès

        // Non authentifié → toujours redirigé (vers /login ou succès si UNAUTHENTICATED)
        mockMvc.perform(requestBuilders)
                .andExpect(expectedBlocked);

        var expected = authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)
                ? expectedSuccess : expectedBlocked;

        mockMvc.perform(requestBuilders.with(user("PAX").roles("MEMBER")))
                .andExpect(expected);

        if (authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = expectedSuccess;
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                        .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED"))))
                .andExpect(expected);

        if (authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = expectedSuccess;
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                        .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED"))))
                .andExpect(expected);

        if (authorization.equals(AuthorizationTest.ADMIN)) {
            expected = expectedSuccess;
        }
        mockMvc.perform(requestBuilders.with(user("PAX")
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED"))))
                .andExpect(expected);
    }
}