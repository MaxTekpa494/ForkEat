package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUpdateServiceTest {

    @Mock
    private UserPersistence userPersistence;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserUpdateService userUpdateService;

    private User createTestUser(UUID id, String username, String email) {
        return new User(
                id,
                username,
                "John",
                "Doe",
                email,
                "hashedPassword123",
                Instant.now(),
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                UUID.randomUUID()
        );
    }

    @Nested
    class UpdateProfileTests {

        @Test
        void updateProfile_ShouldUpdateUserProfile_WhenValidData() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "oldusername", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(userPersistence.existsByUsername("newusername")).thenReturn(false);
            when(userPersistence.saveUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            User result = userUpdateService.updateProfile(
                    userId, "NewFirst", "NewLast", "newusername"
            );

            // Then
            assertEquals("NewFirst", result.firstName());
            assertEquals("NewLast", result.lastName());
            assertEquals("newusername", result.username());
            assertEquals("test@example.com", result.email()); // Email ne change pas
            verify(userPersistence).saveUser(any(User.class));
        }

        @Test
        void updateProfile_ShouldNotCheckUsername_WhenUsernameNotChanged() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "sameusername", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(userPersistence.saveUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            userUpdateService.updateProfile(userId, "First", "Last", "sameusername");

            // Then
            verify(userPersistence, never()).existsByUsername(anyString());
        }

        @Test
        void updateProfile_ShouldThrow_WhenUsernameAlreadyTaken() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "oldusername", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(userPersistence.existsByUsername("takenusername")).thenReturn(true);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userUpdateService.updateProfile(userId, "First", "Last", "takenusername")
            );

            assertEquals("This username is already used", exception.getMessage());
            verify(userPersistence, never()).saveUser(any());
        }

        @Test
        void updateProfile_ShouldThrow_WhenUserNotFound() throws ResourceNotFoundException {
            // Given
            var unknownId = UUID.randomUUID();
            when(userQueryService.getUserById(unknownId))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userUpdateService.updateProfile(unknownId, "First", "Last", "username")
            );
        }
    }

    @Nested
    class UpdateEmailTests {

        @Test
        void updateEmail_ShouldUpdateEmail_WhenPasswordCorrect() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(passwordEncoder.matches("correctPassword", "hashedPassword123")).thenReturn(true);
            when(userPersistence.existsByEmail("new@example.com")).thenReturn(false);
            when(userPersistence.saveUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            User result = userUpdateService.updateEmail(userId, "new@example.com", "correctPassword");

            // Then
            assertEquals("new@example.com", result.email());
            verify(userPersistence).saveUser(any(User.class));
        }

        @Test
        void updateEmail_ShouldThrow_WhenPasswordIncorrect() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(passwordEncoder.matches("wrongPassword", "hashedPassword123")).thenReturn(false);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userUpdateService.updateEmail(userId, "new@example.com", "wrongPassword")
            );

            assertEquals("Mot de passe incorrect", exception.getMessage());
            verify(userPersistence, never()).saveUser(any());
        }

        @Test
        void updateEmail_ShouldThrow_WhenNewEmailAlreadyUsed() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(passwordEncoder.matches("correctPassword", "hashedPassword123")).thenReturn(true);
            when(userPersistence.existsByEmail("taken@example.com")).thenReturn(true);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userUpdateService.updateEmail(userId, "taken@example.com", "correctPassword")
            );

            assertEquals("Cet email est déjà utilisé", exception.getMessage());
            verify(userPersistence, never()).saveUser(any());
        }

        @Test
        void updateEmail_ShouldNotCheckEmail_WhenEmailNotChanged() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "same@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(passwordEncoder.matches("correctPassword", "hashedPassword123")).thenReturn(true);
            when(userPersistence.saveUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            userUpdateService.updateEmail(userId, "same@example.com", "correctPassword");

            // Then
            verify(userPersistence, never()).existsByEmail(anyString());
        }
    }

    @Nested
    class UpdatePasswordTests {

        @Test
        void updatePassword_ShouldUpdatePassword_WhenCurrentPasswordCorrect() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(passwordEncoder.matches("currentPassword", "hashedPassword123")).thenReturn(true);
            when(passwordEncoder.encode("newPassword123")).thenReturn("newHashedPassword");

            // When
            userUpdateService.updatePassword(userId, "currentPassword", "newPassword123");

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userPersistence).saveUser(userCaptor.capture());
            assertEquals("newHashedPassword", userCaptor.getValue().password());
        }

        @Test
        void updatePassword_ShouldThrow_WhenCurrentPasswordIncorrect() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(passwordEncoder.matches("wrongPassword", "hashedPassword123")).thenReturn(false);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userUpdateService.updatePassword(userId, "wrongPassword", "newPassword123")
            );

            assertEquals("Mot de passe actuel incorrect", exception.getMessage());
            verify(userPersistence, never()).saveUser(any());
        }

        @Test
        void updatePassword_ShouldThrow_WhenNewPasswordTooShort() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(passwordEncoder.matches("currentPassword", "hashedPassword123")).thenReturn(true);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userUpdateService.updatePassword(userId, "currentPassword", "short")
            );

            assertEquals("Le nouveau mot de passe doit contenir au moins 8 caractères", exception.getMessage());
            verify(userPersistence, never()).saveUser(any());
        }

        @Test
        void updatePassword_ShouldAcceptPasswordWithExactly8Characters() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(passwordEncoder.matches("currentPassword", "hashedPassword123")).thenReturn(true);
            when(passwordEncoder.encode("pass1234")).thenReturn("newHashedPassword");

            // When
            userUpdateService.updatePassword(userId, "currentPassword", "pass1234");

            // Then
            verify(userPersistence).saveUser(any(User.class));
        }
    }

    @Nested
    class MigrateToOAuth2Tests {

        @Test
        void migrateToOAuth2_ShouldUpdateAuthModeAndRemovePassword() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(userPersistence.saveUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            User result = userUpdateService.migrateToOAuth2(userId, AuthMode.GOOGLE);

            // Then
            assertNull(result.password());
            assertEquals(AuthMode.GOOGLE, result.authentificationMode());
            verify(userPersistence).saveUser(any(User.class));
        }

        @Test
        void migrateToOAuth2_ShouldPreserveOtherUserFields() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var walletId = UUID.randomUUID();
            var existingUser = new User(
                    userId,
                    "testuser",
                    "John",
                    "Doe",
                    "test@example.com",
                    "hashedPassword123",
                    Instant.now(),
                    UserRole.MEMBER,
                    UserStatus.ACTIVE,
                    AuthMode.LOCAL,
                    walletId
            );

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(userPersistence.saveUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            User result = userUpdateService.migrateToOAuth2(userId, AuthMode.GOOGLE);

            // Then
            assertEquals(userId, result.id());
            assertEquals("testuser", result.username());
            assertEquals("John", result.firstName());
            assertEquals("Doe", result.lastName());
            assertEquals("test@example.com", result.email());
            assertEquals(UserRole.MEMBER, result.role());
            assertEquals(UserStatus.ACTIVE, result.status());
            assertEquals(walletId, result.walletId());
        }

        @Test
        void migrateToOAuth2_ShouldThrow_WhenUserNotFound() throws ResourceNotFoundException {
            // Given
            var unknownId = UUID.randomUUID();
            when(userQueryService.getUserById(unknownId))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userUpdateService.migrateToOAuth2(unknownId, AuthMode.GOOGLE)
            );
        }
    }
}