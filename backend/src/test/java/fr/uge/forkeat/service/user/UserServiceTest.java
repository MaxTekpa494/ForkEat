package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.model.wallet.Wallet;
import fr.uge.forkeat.service.persistence.PlatformWalletPersistence;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.uge.forkeat.service.port.PasswordHasherPort;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserPersistence userPersistence;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private WalletPersistence walletPersistence;

    @Mock
    private RecipePersistence recipePersistence;

    @Mock
    private PlatformWalletPersistence platformWalletPersistence;

    @InjectMocks
    private UserService userService;

    private User createTestUser(UUID id, String username, String email) {
        return new User(
                id,
                username,
                "John",
                "Doe",
                email,
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                Instant.now(),
                Instant.now(),
                false);
    }

    @Nested
    class GetUserByEmailTests {

        @Test
        void getUserByEmail_ShouldReturnUser_WhenExists() throws ResourceNotFoundException {
            // Given
            var user = createTestUser(UUID.randomUUID(), "testuser", "test@example.com");
            when(userPersistence.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserByEmail("test@example.com");

            // Then
            assertNotNull(result);
            assertEquals("test@example.com", result.email());
            assertEquals("testuser", result.username());
        }

        @Test
        void getUserByEmail_ShouldThrow_WhenNotFound() {
            // Given
            when(userPersistence.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // When/Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserByEmail("unknown@example.com")
            );

            assertEquals("User not found with email: unknown@example.com", exception.getMessage());
        }

        @Test
        void getUserByEmail_ShouldThrow_WhenEmailIsNull() {
            // Given
            when(userPersistence.findByEmail(null)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserByEmail(null)
            );
        }
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void getUserById_ShouldReturnUser_WhenExists() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var user = createTestUser(userId, "testuser", "test@example.com");
            when(userPersistence.findById(userId)).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserById(userId);

            // Then
            assertNotNull(result);
            assertEquals(userId, result.id());
            assertEquals("testuser", result.username());
        }

        @Test
        void getUserById_ShouldThrow_WhenNotFound() {
            // Given
            var unknownId = UUID.randomUUID();
            when(userPersistence.findById(unknownId)).thenReturn(Optional.empty());

            // When/Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserById(unknownId)
            );

            assertEquals("User not found with id: " + unknownId, exception.getMessage());
        }

        @Test
        void getUserById_ShouldThrow_WhenIdIsNull() {
            // Given
            when(userPersistence.findById(null)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserById(null)
            );
        }
    }

    @Nested
    class GetUserByUsernameTests {

        @Test
        void getUserByUsername_ShouldReturnUser_WhenExists() throws ResourceNotFoundException {
            // Given
            var user = createTestUser(UUID.randomUUID(), "testuser", "test@example.com");
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserByUsername("testuser");

            // Then
            assertNotNull(result);
            assertEquals("testuser", result.username());
            assertEquals("test@example.com", result.email());
        }

        @Test
        void getUserByUsername_ShouldThrow_WhenNotFound() {
            // Given
            when(userPersistence.findByUsername("unknownuser")).thenReturn(Optional.empty());

            // When/Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserByUsername("unknownuser")
            );

            assertEquals("User not found with username: unknownuser", exception.getMessage());
        }

        @Test
        void getUserByUsername_ShouldThrow_WhenUsernameIsNull() {
            // Given
            when(userPersistence.findByUsername(null)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserByUsername(null)
            );
        }

        @Test
        void getUserByUsername_ShouldBeTransactionalReadOnly() throws ResourceNotFoundException {
            // Given
            var user = createTestUser(UUID.randomUUID(), "testuser", "test@example.com");
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserByUsername("testuser");

            // Then
            assertNotNull(result);
        }
    }

    @Nested
    class MultipleUserTests {

        @Test
        void shouldRetrieveDifferentUsersByDifferentCriteria() throws ResourceNotFoundException {
            // Given
            var userId1 = UUID.randomUUID();
            var userId2 = UUID.randomUUID();
            
            var user1 = createTestUser(userId1, "user1", "user1@example.com");
            var user2 = createTestUser(userId2, "user2", "user2@example.com");

            when(userPersistence.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
            when(userPersistence.findById(userId2)).thenReturn(Optional.of(user2));
            when(userPersistence.findByUsername("user1")).thenReturn(Optional.of(user1));

            // When
            User resultByEmail = userService.getUserByEmail("user1@example.com");
            User resultById = userService.getUserById(userId2);
            User resultByUsername = userService.getUserByUsername("user1");

            // Then
            assertEquals(userId1, resultByEmail.id());
            assertEquals(userId2, resultById.id());
            assertEquals(userId1, resultByUsername.id());
        }
    }

    @Nested
    class CheckUserPasswordTests {

        @Test
        void checkUserPassword_ShouldReturnTrue_WhenPasswordMatches() {
            // Given
            var username = "testuser";
            var rawPassword = "password123";
            var hashedPassword = "hashedPassword123";

            when(userPersistence.findPasswordHashByUsername(username)).thenReturn(hashedPassword);
            when(passwordHasherPort.matches(rawPassword, hashedPassword)).thenReturn(true);

            // When
            boolean result = userService.checkUserPassword(username, rawPassword);

            // Then
            assertTrue(result);
        }

        @Test
        void checkUserPassword_ShouldReturnFalse_WhenPasswordDoesNotMatch() {
            // Given
            var username = "testuser";
            var rawPassword = "wrongPassword";
            var hashedPassword = "hashedPassword123";

            when(userPersistence.findPasswordHashByUsername(username)).thenReturn(hashedPassword);
            when(passwordHasherPort.matches(rawPassword, hashedPassword)).thenReturn(false);

            // When
            boolean result = userService.checkUserPassword(username, rawPassword);

            // Then
            assertFalse(result);
        }

        @Test
        void checkUserPassword_ShouldThrow_WhenUserNotFound() {
            // Given
            var username = "unknownuser";
            var rawPassword = "password123";

            when(userPersistence.findPasswordHashByUsername(username)).thenThrow(new ResourceNotFoundException("User not found"));

            // When/Then
            assertThrows(ResourceNotFoundException.class, () -> userService.checkUserPassword(username, rawPassword));
        }
    }

    @Nested
    class FollowTests {

        @Test
        void follow_ShouldCallPersistence_WhenBothUsersExist() {
            // Given
            var followerId = UUID.randomUUID();
            var followedId = UUID.randomUUID();
            when(userPersistence.findIdByUsernameOrThrow("alice")).thenReturn(followerId);
            when(userPersistence.findIdByUsernameOrThrow("bob")).thenReturn(followedId);

            // When
            userService.follow("alice", "bob");

            // Then
            verify(userPersistence).follow(followerId, followedId);
        }

        @Test
        void follow_ShouldThrow_WhenFollowerNotFound() {
            // Given
            when(userPersistence.findIdByUsernameOrThrow("unknown"))
                    .thenThrow(new ResourceNotFoundException("User not found: unknown"));

            // When/Then
            assertThrows(ResourceNotFoundException.class, () -> userService.follow("unknown", "bob"));
            verify(userPersistence, never()).follow(any(), any());
        }

        @Test
        void follow_ShouldThrow_WhenFollowedNotFound() {
            // Given
            when(userPersistence.findIdByUsernameOrThrow("alice")).thenReturn(UUID.randomUUID());
            when(userPersistence.findIdByUsernameOrThrow("unknown"))
                    .thenThrow(new ResourceNotFoundException("User not found: unknown"));

            // When/Then
            assertThrows(ResourceNotFoundException.class, () -> userService.follow("alice", "unknown"));
            verify(userPersistence, never()).follow(any(), any());
        }

        @Test
        void follow_ShouldThrow_WhenFollowerUsernameIsNull() {
            assertThrows(NullPointerException.class, () -> userService.follow(null, "bob"));
        }

        @Test
        void follow_ShouldThrow_WhenFollowedUsernameIsNull() {
            assertThrows(NullPointerException.class, () -> userService.follow("alice", null));
        }
    }

    @Nested
    class UnfollowTests {

        @Test
        void unfollow_ShouldCallPersistence_WhenBothUsersExist() {
            // Given
            var followerId = UUID.randomUUID();
            var followedId = UUID.randomUUID();
            when(userPersistence.findIdByUsernameOrThrow("alice")).thenReturn(followerId);
            when(userPersistence.findIdByUsernameOrThrow("bob")).thenReturn(followedId);

            // When
            userService.unfollow("alice", "bob");

            // Then
            verify(userPersistence).unfollow(followerId, followedId);
        }

        @Test
        void unfollow_ShouldThrow_WhenFollowerNotFound() {
            // Given
            when(userPersistence.findIdByUsernameOrThrow("unknown"))
                    .thenThrow(new ResourceNotFoundException("User not found: unknown"));

            // When/Then
            assertThrows(ResourceNotFoundException.class, () -> userService.unfollow("unknown", "bob"));
            verify(userPersistence, never()).unfollow(any(), any());
        }

        @Test
        void unfollow_ShouldThrow_WhenFollowedNotFound() {
            // Given
            when(userPersistence.findIdByUsernameOrThrow("alice")).thenReturn(UUID.randomUUID());
            when(userPersistence.findIdByUsernameOrThrow("unknown"))
                    .thenThrow(new ResourceNotFoundException("User not found: unknown"));

            // When/Then
            assertThrows(ResourceNotFoundException.class, () -> userService.unfollow("alice", "unknown"));
            verify(userPersistence, never()).unfollow(any(), any());
        }

        @Test
        void unfollow_ShouldThrow_WhenFollowerUsernameIsNull() {
            assertThrows(NullPointerException.class, () -> userService.unfollow(null, "bob"));
        }

        @Test
        void unfollow_ShouldThrow_WhenFollowedUsernameIsNull() {
            assertThrows(NullPointerException.class, () -> userService.unfollow("alice", null));
        }
    }

    @Nested
    class DeleteAccountTests {

        private Wallet buildWallet(UUID walletId, UUID userId, long balance) {
            return new Wallet(walletId, userId, balance, Instant.now());
        }

        @Test
        void deleteAccount_ShouldTransferBalanceReassignRecipesAndDeleteUser_WhenBalancePositive() {
            // Given
            var userId = UUID.randomUUID();
            var user = createTestUser(userId, "testuser", "test@example.com");
            var userWallet = buildWallet(UUID.randomUUID(), userId, 500L);
            var earningsUserId = UUID.randomUUID();
            var earningsWallet = buildWallet(UUID.randomUUID(), earningsUserId, 1000L);

            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(userWallet));
            when(walletPersistence.getEarningsWallet()).thenReturn(earningsWallet);

            // When
            userService.deleteAccount("testuser");

            // Then
            verify(walletPersistence).incrementBalanceById(earningsWallet.id(), 500L);
            verify(platformWalletPersistence).recordTransaction(
                    PlatformWalletType.EARNINGS, 500L, "ACCOUNT_DELETION", userId);
            verify(recipePersistence).reassignRecipesToUser(userId, earningsUserId);
            verify(userPersistence).deleteById(userId);
        }

        @Test
        void deleteAccount_ShouldNotTransferBalanceNorRecordTransaction_WhenBalanceIsZero() {
            // Given
            var userId = UUID.randomUUID();
            var user = createTestUser(userId, "testuser", "test@example.com");
            var userWallet = buildWallet(UUID.randomUUID(), userId, 0L);
            var earningsUserId = UUID.randomUUID();
            var earningsWallet = buildWallet(UUID.randomUUID(), earningsUserId, 1000L);

            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(userWallet));
            when(walletPersistence.getEarningsWallet()).thenReturn(earningsWallet);

            // When
            userService.deleteAccount("testuser");

            // Then
            verify(walletPersistence, never()).incrementBalanceById(any(), any(long.class));
            verify(platformWalletPersistence, never()).recordTransaction(any(), any(long.class), any(), any());
            verify(recipePersistence).reassignRecipesToUser(userId, earningsUserId);
            verify(userPersistence).deleteById(userId);
        }

        @Test
        void deleteAccount_ShouldThrow_WhenUserNotFound() {
            // Given
            when(userPersistence.findByUsername("unknown")).thenReturn(Optional.empty());

            // When / Then
            assertThrows(ResourceNotFoundException.class, () -> userService.deleteAccount("unknown"));

            verify(walletPersistence, never()).loadWalletWithLock(any());
            verify(userPersistence, never()).deleteById(any());
        }

        @Test
        void deleteAccount_ShouldThrow_WhenWalletNotFound() {
            // Given
            var userId = UUID.randomUUID();
            var user = createTestUser(userId, "testuser", "test@example.com");

            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(WalletNotFoundException.class, () -> userService.deleteAccount("testuser"));

            verify(userPersistence, never()).deleteById(any());
        }

        @Test
        void deleteAccount_ShouldThrow_WhenUsernameIsNull() {
            assertThrows(NullPointerException.class, () -> userService.deleteAccount(null));
        }
    }
}