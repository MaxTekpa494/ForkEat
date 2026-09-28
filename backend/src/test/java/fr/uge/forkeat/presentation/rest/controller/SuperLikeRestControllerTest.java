package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.InsufficientFundsException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(SuperLikeRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class SuperLikeRestControllerTest {

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private PromotionService promotionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private SuperLikeRestController superLikeController;

    private final MockMvc mockMvc;

    @Autowired
    SuperLikeRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        superLikeController = new SuperLikeRestController(recipeService, promotionService, authPort, userService);
    }

    private User createUser(UUID id) {
        return new User(id, "testuser", "Test", "User", "test@test.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, Instant.now(), Instant.now(), false);
    }

    @Nested
    class SuperLike {

        @Test
        void shouldReturnOkWhenSuperLikeGoesWell() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenReturn(user);
            doNothing().when(recipeService).superLikeRecipe(any(), any());
            when(authPort.extractUsername()).thenReturn(user.username());

            var response = superLikeController.superLikeRecipe(UUID.randomUUID());
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenThrow(new ResourceNotFoundException("User not found"));
            when(authPort.extractUsername()).thenReturn(user.username());

            assertThrows(ResourceNotFoundException.class, () -> superLikeController.superLikeRecipe(UUID.randomUUID()));
        }

        @Test
        void shouldThrowWhenRecipeNotFound() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenReturn(user);
            doThrow(new ResourceNotFoundException("Recipe not found")).when(recipeService).superLikeRecipe(any(), any());
            when(authPort.extractUsername()).thenReturn(user.username());

            assertThrows(ResourceNotFoundException.class, () -> superLikeController.superLikeRecipe(UUID.randomUUID()));
        }

        @Test
        void shouldThrowInsufficientFundsExceptionWhenBalanceTooLow() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenReturn(user);
            doThrow(new InsufficientFundsException(50L, 100L)).when(recipeService).superLikeRecipe(any(), any());
            when(authPort.extractUsername()).thenReturn(user.username());

            assertThrows(InsufficientFundsException.class, () -> superLikeController.superLikeRecipe(UUID.randomUUID()));
        }
    }
}
