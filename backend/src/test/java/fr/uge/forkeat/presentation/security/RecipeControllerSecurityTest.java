package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.config.SecurityConfig;
import fr.uge.forkeat.infrastructure.security.AuthenticationAdapter;
import fr.uge.forkeat.presentation.rest.controller.RecipeRestController;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.Recipe;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
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
        lenient().when(recipeService.findPersonalizedRecipeById(any(), any())).thenReturn(null);
        lenient().when(recipeService.searchRecipes(any())).thenReturn(null);
        lenient().when(recipeService.findAllAllergens()).thenReturn(List.of());
        lenient().when(recipeService.findAllIngredientNames()).thenReturn(List.of());
        lenient().when(recipeService.findAllDietaryNames()).thenReturn(List.of());
        lenient().when(recipeService.createRecipe(any(), any())).thenReturn(null);
        lenient().when(recipeService.findById(any())).thenReturn(fakeRecipe);
        lenient().when(authPort.extractUsername()).thenReturn("testuser");
        lenient().when(authPort.isAdmin()).thenReturn(false);
        lenient().when(userService.getUserByUsername(any())).thenReturn(null);
        // Par défaut : l'utilisateur peut modifier sa recette
        lenient().when(securityService.canUpdateRecipe(any())).thenReturn(true);
    }


    ////////////////////////////////////
    ///      UNAUTHENTICATED         ///
    ////////////////////////////////////


    @Nested
    class DeleteRecipe {

        @Test
        @WithAnonymousUser
        void shouldReturn401_whenAnonymous() throws Exception {
            mockMvc.perform(post("/api/recipes/{id}/delete", RECIPE_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testRecipeDeleteAuthorization() throws Exception {

            mockMvc.perform(post("/api/recipes/{id}/delete", RECIPE_ID).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED"))))
                    .andExpect(status().isOk());
        }

        private void testRightsPost(String url, AuthorizationTest authorization) throws Exception {
            var expected = status().isForbidden();

            var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().isOk() : status().isUnauthorized();
            mockMvc.perform(post(url)).andExpect(expectedForUnauthenticated);

            if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
                expected = status().isOk();
            }
            mockMvc.perform(post(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

            if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
                expected = status().isOk();
            }
            mockMvc.perform(post(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


            if(authorization.equals(AuthorizationTest.MODERATOR)) {
                expected = status().isOk();
            }
            mockMvc.perform(post(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

            if(authorization.equals(AuthorizationTest.ADMIN)) {
                expected = status().isOk();
            }
            mockMvc.perform(post(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
        }

        private void testRightsPut(String url, AuthorizationTest authorization) throws Exception {
            var expected = status().isForbidden();

            var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().isOk() : status().isUnauthorized();
            mockMvc.perform(put(url)).andExpect(expectedForUnauthenticated);

            if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
                expected = status().isOk();
            }
            mockMvc.perform(put(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

            if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
                expected = status().isOk();
            }
            mockMvc.perform(put(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


            if(authorization.equals(AuthorizationTest.MODERATOR)) {
                expected = status().isOk();
            }
            mockMvc.perform(put(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

            if(authorization.equals(AuthorizationTest.ADMIN)) {
                expected = status().isOk();
            }
            mockMvc.perform(put(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
        }


        private void testRightsDelete(String url, AuthorizationTest authorization) throws Exception {
            var expected = status().isForbidden();

            var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().isOk() : status().isUnauthorized();
            mockMvc.perform(delete(url)).andExpect(expectedForUnauthenticated);

            if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
                expected = status().isOk();
            }
            mockMvc.perform(delete(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

            if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
                expected = status().isOk();
            }
            mockMvc.perform(delete(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


            if(authorization.equals(AuthorizationTest.MODERATOR)) {
                expected = status().isOk();
            }
            mockMvc.perform(delete(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

            if(authorization.equals(AuthorizationTest.ADMIN)) {
                expected = status().isOk();
            }
            mockMvc.perform(delete(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
        }


        private void testRightsGet(String url, AuthorizationTest authorization) throws Exception {
            var expected = status().isForbidden();

            var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED) ? status().isOk() : status().isUnauthorized();
            mockMvc.perform(get(url)).andExpect(expectedForUnauthenticated);

            if(authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
                expected = status().isOk();
            }
            mockMvc.perform(get(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

            if(authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
                expected = status().isOk();
            }
            mockMvc.perform(get(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);


            if(authorization.equals(AuthorizationTest.MODERATOR)) {
                expected = status().isOk();
            }
            mockMvc.perform(get(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

            if(authorization.equals(AuthorizationTest.ADMIN)) {
                expected = status().isOk();
            }
            mockMvc.perform(get(url).with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
        }
    }
}
