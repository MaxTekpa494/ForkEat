package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Transactional
public class SuperLikeControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private PromotionService promotionService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private UserService userService;

    private UUID recipeId;

    @BeforeEach
    void setup() {
        recipeId = UUID.randomUUID();
        var mockUser = createUser(UUID.randomUUID());

        when(authPort.extractUsername()).thenReturn("PAX");
        when(userService.getUserByUsername("PAX")).thenReturn(mockUser);

        doNothing().when(recipeService).superLikeRecipe(any(), any());
        when(promotionService.findSuperLikeHistory(any())).thenReturn(List.of());
        when(promotionService.getConfig()).thenReturn(new SuperLikeConfig(UUID.randomUUID(), 1, new BigDecimal("0.3"), Instant.now()));
    }

    @Nested
    class SuperLikeRestControllerSecurityTests {

        @Test
        void testSuperLikeRecipe() throws Exception {
            testRights(post("/api/super-likes/" + recipeId), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testGetSuperLikeHistory() throws Exception {
            testRights(get("/api/super-likes/history"), AuthorizationTest.EMAIL_VERIFIED);
        }

        @Test
        void testGetSuperLikePrice() throws Exception{
            testRights(get("/api/super-likes/price"), AuthorizationTest.UNAUTHENTICATED);
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

    private User createUser(UUID id) {
        return new User(id, "PaxGPT", "Pax", "Pekpa", "a@gmail.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }
}