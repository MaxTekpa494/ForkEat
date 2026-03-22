package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.config.SecurityConfig;
import fr.uge.forkeat.infrastructure.security.AuthenticationAdapter;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDiff;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.rest.controller.RecipeRestController;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;
import fr.uge.forkeat.service.model.recipe.projection.*;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.security.SecurityService;
import fr.uge.forkeat.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Transactional
public class RecipeControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock toutes les dépendances du controller pour éviter le code métier
    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private UserService userService;

    // Mock du SecurityService utilisé dans @PreAuthorize
    @MockitoBean
    private SecurityService securityService;

    private final UUID RECIPE_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        // Stub permissif pour éviter les NPE dans le code métier
        Recipe fakeRecipe = mock(Recipe.class);
        lenient().when(fakeRecipe.usernameAuthor()).thenReturn("testuser");
        lenient().when(fakeRecipe.status()).thenReturn(RecipeStatus.PUBLISHED);
        lenient().when(recipeService.updateRecipe(any(), any(), any())).thenReturn(getRecipeModel());
        lenient().when(recipeService.findPersonalizedRecipeById(any(), any())).thenReturn(createPersonalizedRecipe());
        lenient().when(recipeService.searchRecipes(any())).thenReturn(createPageResult());
        lenient().when(recipeService.findAllAllergens()).thenReturn(List.of());
        lenient().when(recipeService.findAllIngredientNames()).thenReturn(List.of());
        lenient().when(recipeService.findAllDietaryNames()).thenReturn(List.of());
        lenient().when(recipeService.createRecipe(any(), any())).thenReturn(getRecipeModel());
        lenient().when(recipeService.findById(any())).thenReturn(fakeRecipe);
        lenient().when(authPort.extractUsername()).thenReturn("testuser");
        lenient().when(authPort.isAdmin()).thenReturn(false);
        lenient().when(userService.getUserByUsername(any())).thenReturn(createUser(UUID.randomUUID()));
        lenient().when(securityService.canDeleteRecipe(any())).thenReturn(true);
        lenient().when(recipeService.findRecipesByAuthor(any(), any(), anyInt(), anyInt())).thenReturn(new AuthorRecipesPage(new UserRecipeStats(0, 0, 0, 0), new PageResult<AuthorRecipeSummary>(List.of(),0)));
        // Par défaut : l'utilisateur peut modifier sa recette
        lenient().when(securityService.canUpdateRecipe(any())).thenReturn(true);
    }


    ////////////////////////////////////
    ///      UNAUTHENTICATED         ///
    ////////////////////////////////////


    @Nested
    class RecipeController {

        private static final ObjectMapper mapper = new ObjectMapper();

        @Test
        void testRecipeDeleteAuthorization() throws Exception {
            testRights(post("/api/recipes/{id}/delete", RECIPE_ID), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testGetRecipe() throws Exception {
            try (MockedStatic<RecipeDTOMapper> mock = mockStatic(RecipeDTOMapper.class);
                    MockedStatic<RecipeDiff> mock2 = mockStatic(RecipeDiff.class)) {
                mock.when(() -> RecipeDTOMapper.toDTO((Recipe) any()))
                        .thenReturn(null);

                mock2.when(()->RecipeDiff.compute(any(), any())).thenReturn(null);
                testRights(get("/api/recipes/{id}", RECIPE_ID), AuthorizationTest.UNAUTHENTICATED);
            }
        }

        @Test
        void testGetRecipes() throws Exception {
            testRights(get("/api/recipes"), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testGetAllergens() throws Exception {
            testRights(get("/api/recipes/allergens"), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testGetPageCreateRecipe() throws Exception {
            testRights(get("/api/recipes/create"), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testCreateRecipe() throws Exception {
            try (MockedStatic<RecipeDTOMapper> mock = mockStatic(RecipeDTOMapper.class)) {
                mock.when(() -> RecipeDTOMapper.toDTO((Recipe) any()))
                        .thenReturn(null);

                // ton test ici

                testRightsMultiPart(multipart("/api/recipes/create").file(new MockMultipartFile(
                        "recipe",           // nom du @RequestPart
                        "",                 // filename
                        "application/json", // content-type
                        mapper.writeValueAsString(getRecipe()).getBytes()
                )), AuthorizationTest.EMAIL_VERIFIED);
            }
        }

        @Test
        void testUpdateRecipe() throws Exception {

            try (MockedStatic<RecipeDTOMapper> mock = mockStatic(RecipeDTOMapper.class)) {
                mock.when(() -> RecipeDTOMapper.toDTO((Recipe) any()))
                        .thenReturn(getRecipe());

                testRightsMultiPart(multipart("/api/recipes/{id}/update", RECIPE_ID).file(new MockMultipartFile(
                        "recipe",           // nom du @RequestPart
                        "",                 // filename
                        "application/json", // content-type
                        mapper.writeValueAsString(getRecipe()).getBytes()
                )), AuthorizationTest.EMAIL_VERIFIED);
            }
        }

        @Test
        void testDeleteRecipe() throws Exception {
            testRights(post("/api/recipes/{id}/delete", RECIPE_ID), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testMyRecipes() throws Exception {
            testRights(get("/api/recipes/my-recipes"), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testCreateVariant() throws Exception {
            try (MockedStatic<RecipeDTOMapper> mock = mockStatic(RecipeDTOMapper.class)) {
                mock.when(() -> RecipeDTOMapper.toDTO((Recipe) any()))
                        .thenReturn(getRecipe());
                testRightsMultiPart(multipart("/api/recipes/create-variant").file(new MockMultipartFile(
                        "recipe",           // nom du @RequestPart
                        "",                 // filename
                        "application/json", // content-type
                        mapper.writeValueAsString(getRecipe()).getBytes()
                )), AuthorizationTest.EMAIL_VERIFIED);
            }
        }

        @Test
        void testLikeRecipe() throws Exception {
            testRights(post("/api/recipes/{id}/like", RECIPE_ID), AuthorizationTest.EMAIL_VERIFIED);
        }


        @Test
        void testUnlikeRecipe() throws Exception {
            testRights(delete("/api/recipes/{id}/like", RECIPE_ID), AuthorizationTest.EMAIL_VERIFIED);
        }
    }

    @Nested
    class RecipeWebControllerSecurityTest {

        @Test
        void testCreateRecipe() throws Exception {
            var dto = getRecipe();
            testRightsMultiPartMVC(multipart("/recipes").param("title", dto.title())
                    .param("summary", dto.summary())
                    .param("preparationMinutes", String.valueOf(dto.preparationMinutes()))
                    .param("status", dto.status()), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testMyRecipes() throws Exception {
            testRightsMVCNoRedirect(get("/recipes/my-recipes"), AuthorizationTest.MEMBER);
        }

        @Test
        void testListRecipes() throws Exception {
            testRightsMVCNoRedirect(get("/recipes"), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testViewRecipe() throws Exception {
            testRightsMVCNoRedirect(get("/recipes/{id}", RECIPE_ID), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testDeleteRecipe() throws Exception {
            testRightsMVC(post("/recipes/{id}/delete", RECIPE_ID), AuthorizationTest.MEMBER);
        }


        @Test
        void testEditRecipeForm() throws Exception {
            testRightsMVCNoRedirect(get("/recipes/{id}/edit", RECIPE_ID), AuthorizationTest.UNAUTHENTICATED);
        }

        @Test
        void testUpdateRecipe() throws Exception {
            var dto = getRecipe();
            testRightsMultiPartMVC(multipart("/recipes/{id}/edit", UUID.randomUUID()).param("title", dto.title())
                    .param("summary", dto.summary())
                    .param("preparationMinutes", String.valueOf(dto.preparationMinutes()))
                    .param("status", dto.status()), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testPageCreateVariant() throws Exception {
            var dto = getRecipe();
            testRightsMultiPartMVC(multipart("/recipes/create-variant").param("title", dto.title())
                    .param("summary", dto.summary())
                    .param("preparationMinutes", String.valueOf(dto.preparationMinutes()))
                    .param("status", dto.status()), AuthorizationTest.EMAIL_VERIFIED);




        }

        @Test
        void testCreateVariant() throws Exception {

            var dto = getRecipe();
            testRightsMultiPartMVC(multipart("/recipes/create-variant")
                    .param("title", dto.title())
                    .param("summary", dto.summary())
                    .param("preparationMinutes", String.valueOf(dto.preparationMinutes()))
                    .param("status", dto.status()), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testLikeRecipe() throws Exception {
            testRightsMVC(post("/recipes/{id}/like", RECIPE_ID), AuthorizationTest.MEMBER);
        }

        @Test
        void testUnlikeRecipe() throws Exception {
            testRightsMVC(post("/recipes/{id}/unlike", RECIPE_ID), AuthorizationTest.MEMBER);
        }

        @Test
        void testSuperLikeRecipe() throws Exception {
            testRightsMVC(post("/recipes/{id}/super-like", RECIPE_ID), AuthorizationTest.MEMBER);
        }
    }

    private Recipe getRecipeModel(){
        return RecipeDTOMapper.toDomain(getRecipe());
    }

    private RecipeDTO getRecipe(){
        return createRecipe(UUID.randomUUID(), "recipe", UUID.randomUUID(), RecipeStatus.PUBLISHED);
    }

    private RecipeDTO createRecipe(UUID id, String title, UUID parentId, RecipeStatus status) {
        return new RecipeDTO(
                id, title, "Summary for " + title, parentId,
                "chef_test", 30, null, status.toString(),
                List.of(), List.of(), List.of(), List.of(), Instant.now(), Instant.now()
        );
    }

    private User createUser(UUID id) {
        return new User(id, "PaxGPT", "Pax", "Pekpa", "a@gmail.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }

    private PersonalizedRecipe createPersonalizedRecipe(){
        return new PersonalizedRecipe(RecipeDTOMapper.toDomain(getRecipe()), RecipeCounts.ZERO, RecipeUserInteraction.NONE);
    }

    private PageResult<PersonalizedRecipeSummary> createPageResult(){
        return new PageResult<>(List.of(), 0);
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



    private void testRightsMultiPartMVC(MockMultipartHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
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

    private void testRightsMultiPartMVCNoRedirect(MockMultipartHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
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


    private void testRightsMultiPart(MockMultipartHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expected = status().isForbidden();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().isOk() : status().isUnauthorized();
        mockMvc.perform(requestBuilders).andExpect(expectedForUnauthenticated);

        if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().isOk();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().isOk();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


        if(authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().isOk();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().isOk();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }

    private void testRights(MockHttpServletRequestBuilder requestBuilders, AuthorizationTest authorization) throws Exception {
        var expected = status().isForbidden();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().isOk() : status().isUnauthorized();
        mockMvc.perform(requestBuilders).andExpect(expectedForUnauthenticated);

        if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().isOk();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().isOk();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


        if(authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().isOk();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if(authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().isOk();
        }
        mockMvc.perform(requestBuilders.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }
}
