package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.service.PlatformWalletService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.wallet.PlatformWallet;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
public class AdminControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private RecipeService recipeService;
    @MockitoBean
    private PlatformWalletService platformWalletService;
    @MockitoBean
    private AuthenticationPort authPort;

    @BeforeEach
    void setup() {

        var mockUser = createUser(UUID.randomUUID());
        var mockUserList = new PageResult<User>(List.of(), 1L);

        List<Recipe> mockRecipes = List.of();
        var mockRecipesPaginated = new PageResult<Recipe>(mockRecipes, 2L);

        var mockEarningsWallet = new PlatformWallet(UUID.randomUUID(), PlatformWalletType.EARNINGS, 10000L, Instant.now());
        var mockRedistributionWallet = new PlatformWallet(UUID.randomUUID(), PlatformWalletType.REDISTRIBUTION, 5000L, Instant.now());

        when(userRegistrationService.registerModerator(any())).thenReturn(mockUser);
        when(userRegistrationService.registerAdmin(any())).thenReturn(mockUser);

        when(userService.getUsersByRole(UserRole.ADMIN)).thenReturn(mockUserList);
        when(userService.getUsersByRole(UserRole.MEMBER)).thenReturn(mockUserList);
        when(userService.getUsersByRole(UserRole.MODERATOR)).thenReturn(mockUserList);
        when(userService.countByRole(UserRole.MEMBER)).thenReturn(10L);
        when(userService.countByRole(UserRole.MODERATOR)).thenReturn(3L);
        when(userService.countByRole(UserRole.ADMIN)).thenReturn(1L);

        when(recipeService.countByStatus(RecipeStatus.PUBLISHED)).thenReturn(50L);
        when(recipeService.countByStatus(RecipeStatus.PENDING_REVIEW)).thenReturn(5L);
        when(recipeService.countByStatus(RecipeStatus.DRAFT)).thenReturn(12L);
        when(recipeService.findByStatus(RecipeStatus.PENDING_REVIEW)).thenReturn(mockRecipes);
        when(recipeService.findByStatus(any(), anyInt(), anyInt())).thenReturn(mockRecipesPaginated);
        when(recipeService.updateStatus(any(), any())).thenReturn(getRecipe());

        when(platformWalletService.getWallet(PlatformWalletType.EARNINGS)).thenReturn(mockEarningsWallet);
        when(platformWalletService.getWallet(PlatformWalletType.REDISTRIBUTION)).thenReturn(mockRedistributionWallet);

        when(authPort.extractUsername()).thenReturn("PaxGPT");
        when(userService.getUserByUsername("PaxGPT")).thenReturn(mockUser);
    }


     @Nested
     class AdminRestControllerSecurityTest{

         @Test
         void testRegisterModerator() throws Exception {
             var mapper = new ObjectMapper();
             testRights(post("/api/admin/register")
                     .contentType(MediaType.APPLICATION_JSON)
                     .content(mapper.writeValueAsString(new UserRegisterDTO("aza", "adel", "ziani", "beaugossedu77", "adel@forkeat.com"))), AuthorizationTest.ADMIN);
         }

         @Test
         void testCreateAdmin() throws Exception {
             var mapper = new ObjectMapper();
             testRights(post("/api/admin/admins")
                     .contentType(MediaType.APPLICATION_JSON)
                     .content(mapper.writeValueAsString(new UserRegisterDTO("aza", "adel", "ziani", "beaugossedu77", "adel@forkeat.com"))), AuthorizationTest.ADMIN);
         }

         @Test
         void testGetAdmins() throws Exception {
             testRights(get("/api/admin/admins"), AuthorizationTest.ADMIN);
         }

         @Test
         void testGetMembers() throws Exception {
             testRights(get("/api/admin/users"), AuthorizationTest.ADMIN);
         }

         @Test
         void testGetModerators() throws Exception {
             testRights(get("/api/admin/moderators"), AuthorizationTest.ADMIN);
         }

         @Test
         void testGetUserStats() throws Exception {
             testRights(get("/api/admin/stats/users"), AuthorizationTest.ADMIN);
         }

         @Test
         void testGetRecipeStats() throws Exception {
             testRights(get("/api/admin/stats/recipes"), AuthorizationTest.ADMIN);
         }


         @Test
         void testGetPublishedRecipes() throws Exception {
             testRights(get("/api/admin/recipes/published"), AuthorizationTest.ADMIN);
         }


         @Test
         void testGetBenefitsWallet() throws Exception {
             testRights(get("/api/admin/wallets/benefits"), AuthorizationTest.ADMIN);
         }

         @Test
         void testGetRedistributionWallet() throws Exception {
             testRights(get("/api/admin/wallets/redistribution"), AuthorizationTest.ADMIN);
         }

         @Test
         void testGetRedistribution() throws Exception {
             testRights(get("/api/admin/wallets/redistribution").content("{\"recipeId\": \""+UUID.randomUUID()+"\", \"month\": \"May\"}"), AuthorizationTest.ADMIN);
         }
     }

    @Nested
    class AdminWebControllerSecurityTest {

        @Test
        void testDashboard() throws Exception {
            testRightsMVCNoRedirect(get("/admin"), AuthorizationTest.ADMIN);
        }

        @Test
        void testUsers() throws Exception {
            testRightsMVCNoRedirect(get("/admin/users")
                    .param("role", "MEMBER"), AuthorizationTest.ADMIN);
        }

        @Test
        void testCreateAdminForm() throws Exception {
            testRightsMVCNoRedirect(get("/admin/create-admin"), AuthorizationTest.ADMIN);
        }

        @Test
        void testCreateAdmin() throws Exception {
            testRightsMVC(post("/admin/create-admin")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", "newadmin")
                    .param("firstName", "John")
                    .param("lastName", "Doe")
                    .param("email", "newadmin@forkeat.com")
                    .param("password", "password123"), AuthorizationTest.ADMIN);
        }

        @Test
        void testWallets() throws Exception {
            testRightsMVCNoRedirect(get("/admin/wallets"), AuthorizationTest.ADMIN);
        }

        @Test
        void testPublishedRecipes() throws Exception {
            testRightsMVCNoRedirect(get("/admin/recipes")
                    .param("page", "0"), AuthorizationTest.ADMIN);
        }

        @Test
        void testPendingRecipes() throws Exception {
            testRightsMVCNoRedirect(get("/admin/recipes/pending"), AuthorizationTest.ADMIN);
        }
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

    private User createUser(UUID id) {
        return new User(id, "PaxGPT", "Pax", "Pekpa", "a@gmail.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }

    private Recipe getRecipe(){
        return createRecipe(UUID.randomUUID(), "recipe", UUID.randomUUID(), RecipeStatus.PUBLISHED);
    }

    private Recipe createRecipe(UUID id, String title, UUID parentId, RecipeStatus status) {
        return new Recipe(
                id, title, "Summary for " + title, parentId,
                "chef_test", 30, null, status,
                List.of(), List.of(), List.of(), List.of(), Instant.now(), Instant.now()
        );
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

}

