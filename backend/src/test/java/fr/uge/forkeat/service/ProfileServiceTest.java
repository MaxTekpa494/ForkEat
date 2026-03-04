package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.projection.UserPublicProfile;
import fr.uge.forkeat.service.model.user.projection.UserSocialStats;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserPersistence userPersistence;

    @Mock
    private RecipePersistence recipePersistence;

    @Mock
    private WalletService walletService;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(userPersistence, recipePersistence, walletService);
    }

    private UserPublicProfile createPublicProfile(String username) {
        return new UserPublicProfile(username, "Jean", "Dupont");
    }

    private UserSocialStats createSocialStats() {
        return new UserSocialStats(10, 5, 20, 3);
    }

    private RecipeSummary createRecipeSummary(UUID id) {
        return new RecipeSummary(id, "Tarte", "Délicieuse tarte", null, 30, Instant.now(), "chef_test");
    }

    private User createUser(String username) {
        return new User(UUID.randomUUID(), username, "Jean", "Dupont", "jean@test.fr",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, Instant.now(), Instant.now(), true);
    }

    @Nested
    class GetProfileInfos {

        @Test
        void shouldReturnProfileWithEmptyRecipes() {
            var username = "chef";
            var currentUsername = "viewer";
            var publicProfile = createPublicProfile(username);
            var socialStats = createSocialStats();

            when(userPersistence.findPublicProfile(username)).thenReturn(publicProfile);
            when(userPersistence.findUserSocialStats(username)).thenReturn(socialStats);
            when(recipePersistence.findUserRecipeSummaries(eq(username), eq(RecipeStatus.PUBLISHED), anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(), 0L));
            when(userPersistence.isFollowing(currentUsername, username)).thenReturn(false);

            var result = service.getProfileInfos(username, 0, 10, currentUsername);

            assertEquals(publicProfile, result.profileWithRecipes().profile().publicProfile());
            assertEquals(socialStats, result.profileWithRecipes().profile().socialStats());
            assertTrue(result.profileWithRecipes().recipes().items().isEmpty());
            assertEquals(0L, result.profileWithRecipes().recipes().total());
            verifyNoInteractions(walletService);
        }

        @Test
        void shouldReturnProfileWithPersonalizedRecipes() {
            var username = "chef";
            var currentUsername = "viewer";
            var recipeId = UUID.randomUUID();
            var publicProfile = createPublicProfile(username);
            var socialStats = createSocialStats();
            var summary = createRecipeSummary(recipeId);
            var interaction = new RecipeUserInteraction(true, false, false);
            var counts = new RecipeCounts(5L, 1L, 0L);

            when(userPersistence.findPublicProfile(username)).thenReturn(publicProfile);
            when(userPersistence.findUserSocialStats(username)).thenReturn(socialStats);
            when(recipePersistence.findUserRecipeSummaries(eq(username), eq(RecipeStatus.PUBLISHED), eq(10), eq(0)))
                    .thenReturn(new PageResult<>(List.of(summary), 1L));
            when(recipePersistence.findRecipeCounts(List.of(recipeId))).thenReturn(Map.of(recipeId, counts));
            when(recipePersistence.findUserRecipeInteractions(List.of(recipeId), currentUsername))
                    .thenReturn(Map.of(recipeId, interaction));
            when(userPersistence.isFollowing(currentUsername, username)).thenReturn(false);

            var result = service.getProfileInfos(username, 0, 10, currentUsername);

            assertEquals(1, result.profileWithRecipes().recipes().items().size());
            var item = result.profileWithRecipes().recipes().items().getFirst();
            assertEquals(summary, item.summary());
            assertEquals(counts, item.counts());
            assertEquals(interaction, item.interaction());
        }

        @Test
        void shouldDefaultToNoneInteraction_WhenNotInMap() {
            var username = "chef";
            var currentUsername = "viewer";
            var recipeId = UUID.randomUUID();
            var summary = createRecipeSummary(recipeId);

            when(userPersistence.findPublicProfile(username)).thenReturn(createPublicProfile(username));
            when(userPersistence.findUserSocialStats(username)).thenReturn(createSocialStats());
            when(recipePersistence.findUserRecipeSummaries(any(), any(), anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(summary), 1L));
            when(recipePersistence.findRecipeCounts(anyList())).thenReturn(Map.of());
            when(recipePersistence.findUserRecipeInteractions(anyList(), any())).thenReturn(Map.of());
            when(userPersistence.isFollowing(currentUsername, username)).thenReturn(false);

            var result = service.getProfileInfos(username, 0, 10, currentUsername);

            var item = result.profileWithRecipes().recipes().items().getFirst();
            assertEquals(RecipeUserInteraction.NONE, item.interaction());
            assertEquals(RecipeCounts.ZERO, item.counts());
        }

        @Test
        void shouldReturnFollowedInteraction_WhenCurrentUserFollows() {
            var username = "chef";
            var currentUsername = "viewer";
            when(userPersistence.findPublicProfile(username)).thenReturn(createPublicProfile(username));
            when(userPersistence.findUserSocialStats(username)).thenReturn(createSocialStats());
            when(recipePersistence.findUserRecipeSummaries(any(), any(), anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(), 0L));
            when(userPersistence.isFollowing(currentUsername, username)).thenReturn(true);

            var result = service.getProfileInfos(username, 0, 10, currentUsername);

            assertTrue(result.followedByCurrentUser());
        }

        @Test
        void shouldThrow_WhenUsernameIsNull() {
            assertThrows(NullPointerException.class,
                    () -> service.getProfileInfos(null, 0, 10, "viewer"));
        }

        @Test
        void shouldThrow_WhenCurrentUsernameIsNull() {
            assertThrows(NullPointerException.class,
                    () -> service.getProfileInfos("chef", 0, 10, null));
        }

        @Test
        void shouldThrow_WhenPageIsNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.getProfileInfos("chef", -1, 10, "viewer"));
        }

        @Test
        void shouldThrow_WhenSizeIsZeroOrNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.getProfileInfos("chef", 0, 0, "viewer"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.getProfileInfos("chef", 0, -1, "viewer"));
        }
    }

    @Nested
    class GetAccountDetails {

        @Test
        void shouldReturnAccountDetails() {
            var username = "chef";
            var user = createUser(username);
            var socialStats = createSocialStats();

            when(userPersistence.findByUsername(username)).thenReturn(Optional.of(user));
            when(userPersistence.findUserSocialStats(username)).thenReturn(socialStats);
            when(walletService.getBalance(user.id())).thenReturn(1000L);
            when(recipePersistence.countByAuthorUsername(username)).thenReturn(7L);

            var result = service.getAccountDetails(username);

            assertEquals(user, result.user());
            assertEquals(socialStats, result.socialStats());
            assertEquals(1000L, result.walletBalance());
            assertEquals(7L, result.recipeCount());
        }

        @Test
        void shouldThrow_WhenUserNotFound() {
            when(userPersistence.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getAccountDetails("unknown"));
        }

        @Test
        void shouldThrow_WhenUsernameIsNull() {
            assertThrows(NullPointerException.class,
                    () -> service.getAccountDetails(null));
        }
    }
}