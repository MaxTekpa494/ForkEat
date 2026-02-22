package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.projection.PersonalizedUserProfile;
import fr.uge.forkeat.service.model.user.projection.UserProfile;
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

    private UserProfile createUserProfile(String username) {
        var publicProfile = new UserPublicProfile(username, "Jean", "Dupont");
        var socialStats = new UserSocialStats(10, 5, 20, 3);
        return new UserProfile(publicProfile, socialStats);
    }

    private RecipeSummary createRecipeSummary(UUID id) {
        return new RecipeSummary(id, "Tarte", "Délicieuse tarte", null, 30, Instant.now(), 5L, 1L);
    }

    private User createUser(String username) {
        return new User(UUID.randomUUID(), username, "Jean", "Dupont", "jean@test.fr",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, Instant.now(), Instant.now(), true);
    }

    @Nested
    class GetProfileInfos {

        private void stubProfileInfos(String username, String currentUsername) {
            when(userPersistence.findUserProfile(username)).thenReturn(createUserProfile(username));
            when(recipePersistence.findRecipeSummaries(eq(username), eq(RecipeStatus.PUBLISHED), anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(), 0L));
            when(userPersistence.isFollowing(currentUsername, username)).thenReturn(false);
        }

        @Test
        void shouldReturnProfileWithEmptyRecipes() {
            stubProfileInfos("chef", "viewer");

            var result = service.getProfileInfos("chef", 0, 10, "viewer");

            assertEquals(createUserProfile("chef"), result.profileWithRecipes().profile());
            assertTrue(result.profileWithRecipes().recipes().items().isEmpty());
            assertEquals(0L, result.profileWithRecipes().recipes().total());
            verifyNoInteractions(walletService);
        }

        @Test
        void shouldReturnProfileWithPersonalizedRecipes() {
            var recipeId = UUID.randomUUID();
            var profile = createUserProfile("chef");
            var summary = createRecipeSummary(recipeId);
            var interaction = new RecipeUserInteraction(true, false);

            when(userPersistence.findUserProfile("chef")).thenReturn(profile);
            when(recipePersistence.findRecipeSummaries(eq("chef"), eq(RecipeStatus.PUBLISHED), eq(10), eq(0)))
                    .thenReturn(new PageResult<>(List.of(summary), 1L));
            when(recipePersistence.findUserRecipeInteractions(List.of(recipeId), "viewer"))
                    .thenReturn(Map.of(recipeId, interaction));
            when(userPersistence.isFollowing("viewer", "chef")).thenReturn(false);

            var result = service.getProfileInfos("chef", 0, 10, "viewer");

            assertEquals(1, result.profileWithRecipes().recipes().items().size());
            var item = result.profileWithRecipes().recipes().items().getFirst();
            assertEquals(summary, item.summary());
            assertTrue(item.likedByCurrentUser());
            assertFalse(item.superLikedByCurrentUser());
        }

        @Test
        void shouldDefaultToNoneInteraction_WhenNotInMap() {
            var recipeId = UUID.randomUUID();
            var summary = createRecipeSummary(recipeId);

            when(userPersistence.findUserProfile("chef")).thenReturn(createUserProfile("chef"));
            when(recipePersistence.findRecipeSummaries(any(), any(), anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(summary), 1L));
            when(recipePersistence.findUserRecipeInteractions(any(), any())).thenReturn(Map.of());
            when(userPersistence.isFollowing("viewer", "chef")).thenReturn(false);

            var result = service.getProfileInfos("chef", 0, 10, "viewer");

            var item = result.profileWithRecipes().recipes().items().getFirst();
            assertFalse(item.likedByCurrentUser());
            assertFalse(item.superLikedByCurrentUser());
        }

        @Test
        void shouldAlwaysRequestPublishedRecipes() {
            stubProfileInfos("chef", "viewer");

            service.getProfileInfos("chef", 0, 10, "viewer");

            verify(recipePersistence).findRecipeSummaries("chef", RecipeStatus.PUBLISHED, 10, 0);
        }

        @Test
        void shouldCallInteractionsWithEmptyList_WhenNoRecipes() {
            stubProfileInfos("chef", "viewer");

            service.getProfileInfos("chef", 0, 10, "viewer");

            verify(recipePersistence).findUserRecipeInteractions(List.of(), "viewer");
        }

        @Test
        void shouldReturnFollowedInteraction_WhenCurrentUserFollows() {
            stubProfileInfos("chef", "viewer");
            when(userPersistence.isFollowing("viewer", "chef")).thenReturn(true);

            var result = service.getProfileInfos("chef", 0, 10, "viewer");

            assertTrue(result.followedByCurrentUser());
        }

        @Test
        void shouldReturnNotFollowedInteraction_WhenCurrentUserDoesNotFollow() {
            stubProfileInfos("chef", "viewer");

            var result = service.getProfileInfos("chef", 0, 10, "viewer");

            assertFalse(result.followedByCurrentUser());
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
            var user = createUser("chef");
            var profile = createUserProfile("chef");
            when(userPersistence.findByUsername("chef")).thenReturn(Optional.of(user));
            when(userPersistence.findUserProfile("chef")).thenReturn(profile);
            when(walletService.getBalance(user.id())).thenReturn(1000L);
            when(recipePersistence.countByAuthorUsername("chef")).thenReturn(7L);

            var result = service.getAccountDetails("chef");

            assertEquals(user, result.user());
            assertEquals(profile.socialStats(), result.socialStats());
            assertEquals(1000L, result.walletBalance());
            assertEquals(7L, result.recipeCount());
        }

        @Test
        void shouldReturnZeroRecipeCount_WhenUserHasNoRecipes() {
            var user = createUser("chef");
            var profile = createUserProfile("chef");
            when(userPersistence.findByUsername("chef")).thenReturn(Optional.of(user));
            when(userPersistence.findUserProfile("chef")).thenReturn(profile);
            when(walletService.getBalance(user.id())).thenReturn(0L);
            when(recipePersistence.countByAuthorUsername("chef")).thenReturn(0L);

            var result = service.getAccountDetails("chef");

            assertEquals(0L, result.recipeCount());
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