package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.CheckProfileUpdateFailure;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                Instant.now(),
                Instant.now()
        );
    }

    @Nested
    class UpdateProfileTests {

        @Test
        void updateProfile_ShouldUpdateUserProfile_WhenValidData() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "oldusername", "test@example.com");

            // Le service appelle getUserByUsername avec le CURRENT username (oldusername)
            when(userQueryService.getUserByUsername("oldusername")).thenReturn(existingUser);
            when(userPersistence.existsByUsername("newusername")).thenReturn(false);
            when(userPersistence.updateUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When - On passe currentUsername puis newUsername
            User result = userUpdateService.updateProfile(
                    "oldusername", "newusername", "NewFirst", "NewLast"
            );

            // Then
            assertEquals("NewFirst", result.firstName());
            assertEquals("NewLast", result.lastName());
            assertEquals("newusername", result.username());
            assertEquals("test@example.com", result.email());
            verify(userQueryService).getUserByUsername("oldusername");
            verify(userPersistence).existsByUsername("newusername");
            verify(userPersistence).updateUser(any(User.class));
        }

        @Test
        void updateProfile_ShouldNotCheckUsername_WhenUsernameNotChanged() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "sameusername", "test@example.com");

            when(userQueryService.getUserByUsername("sameusername")).thenReturn(existingUser);
            when(userPersistence.updateUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When - currentUsername et newUsername sont identiques
            userUpdateService.updateProfile("sameusername", "sameusername", "First", "Last");

            // Then
            verify(userPersistence, never()).existsByUsername(anyString());
            verify(userPersistence).updateUser(any(User.class));
        }

        @Test
        void updateProfile_ShouldThrow_WhenUsernameAlreadyTaken() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "oldusername", "test@example.com");

            // Le service appelle getUserByUsername avec le CURRENT username
            when(userQueryService.getUserByUsername("oldusername")).thenReturn(existingUser);
            when(userPersistence.existsByUsername("takenusername")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updateProfile("oldusername", "takenusername", "First", "Last")
            );

            assertEquals("Ce nom d'utilisateur est déjà pris", exception.getMessage());
            verify(userQueryService).getUserByUsername("oldusername");
            verify(userPersistence).existsByUsername("takenusername");
            verify(userPersistence, never()).updateUser(any());
        }

        @Test
        void updateProfile_ShouldThrow_WhenUserNotFound() {
            // Given
            when(userQueryService.getUserByUsername("unknown"))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userUpdateService.updateProfile("unknown", "newusername", "First", "Last")
            );
        }
    }

    @Nested
    class UpdateEmailTests {

        @Test
        void updateEmail_ShouldUpdateEmail_WhenPasswordCorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.checkPassword("testuser", "correctPassword")).thenReturn(true);
            when(userPersistence.existsByEmail("new@example.com")).thenReturn(false);
            when(userPersistence.updateUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            User result = userUpdateService.updateEmail("testuser", "new@example.com", "correctPassword");

            // Then
            assertEquals("new@example.com", result.email());
            verify(userPersistence).updateUser(any(User.class));
        }

        @Test
        void updateEmail_ShouldThrow_WhenPasswordIncorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.checkPassword("testuser", "wrongPassword")).thenReturn(false);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updateEmail("testuser", "new@example.com", "wrongPassword")
            );

            assertEquals("Incorrect password", exception.getMessage());
            verify(userPersistence, never()).updateUser(any());
        }

        @Test
        void updateEmail_ShouldThrow_WhenNewEmailAlreadyUsed() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.checkPassword("testuser", "correctPassword")).thenReturn(true);
            when(userPersistence.existsByEmail("taken@example.com")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updateEmail("testuser", "taken@example.com", "correctPassword")
            );

            assertEquals("Cet email est déjà utilisé", exception.getMessage());
            verify(userPersistence, never()).updateUser(any());
        }

        @Test
        void updateEmail_ShouldNotCheckEmail_WhenEmailNotChanged() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "same@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.checkPassword("testuser", "correctPassword")).thenReturn(true);
            when(userPersistence.updateUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            userUpdateService.updateEmail("testuser", "same@example.com", "correctPassword");

            // Then
            verify(userPersistence, never()).existsByEmail(anyString());
            verify(userPersistence).updateUser(any(User.class));
        }
    }

    @Nested
    class UpdatePasswordTests {

        @Test
        void updatePassword_ShouldUpdatePassword_WhenCurrentPasswordCorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(passwordEncoder.encode("currentPassword")).thenReturn("encodedCurrentPassword");
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
            when(userPersistence.checkPassword("testuser", "encodedNewPassword")).thenReturn(true);

            // When
            userUpdateService.updatePassword("testuser", "currentPassword", "newPassword123");

            // Then
            verify(userPersistence).saveUser(any(User.class), eq("encodedCurrentPassword"));
        }

        @Test
        void updatePassword_ShouldThrow_WhenCurrentPasswordIncorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(passwordEncoder.encode("wrongPassword")).thenReturn("encodedWrongPassword");
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
            when(userPersistence.checkPassword("testuser", "encodedNewPassword")).thenReturn(false);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updatePassword("testuser", "wrongPassword", "newPassword123")
            );

            assertEquals("Incorrect current password", exception.getMessage());
            verify(userPersistence, never()).saveUser(any(), anyString());
        }

        @Test
        void updatePassword_ShouldThrow_WhenNewPasswordTooShort() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(passwordEncoder.encode("currentPassword")).thenReturn("encodedCurrentPassword");
            when(passwordEncoder.encode("short")).thenReturn("encodedShort");
            when(userPersistence.checkPassword("testuser", "encodedShort")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updatePassword("testuser", "currentPassword", "short")
            );

            assertEquals("New password must be at least 8 characters", exception.getMessage());
            verify(userPersistence, never()).saveUser(any(), anyString());
        }

        @Test
        void updatePassword_ShouldThrow_WhenPasswordsAreSame() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(passwordEncoder.encode("samePassword")).thenReturn("encodedPassword");
            when(userPersistence.checkPassword("testuser", "encodedPassword")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updatePassword("testuser", "samePassword", "samePassword")
            );

            assertEquals("Passwords are the same", exception.getMessage());
            verify(userPersistence, never()).saveUser(any(), anyString());
        }

        @Test
        void updatePassword_ShouldAcceptPasswordWithExactly8Characters() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(passwordEncoder.encode("current8")).thenReturn("encodedCurrent");
            when(passwordEncoder.encode("newpass8")).thenReturn("encodedNew");
            when(userPersistence.checkPassword("testuser", "encodedNew")).thenReturn(true);

            // When
            userUpdateService.updatePassword("testuser", "current8", "newpass8");

            // Then
            verify(userPersistence).saveUser(any(User.class), eq("encodedCurrent"));
        }
    }

    @Nested
    class MigrateToOAuth2Tests {

        @Test
        void migrateToOAuth2_ShouldUpdateAuthMode() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(userPersistence.saveUser(any(User.class), isNull())).thenAnswer(i -> i.getArgument(0));

            // When
            User result = userUpdateService.migrateToOAuth2(userId, AuthMode.GOOGLE);

            // Then
            assertEquals(AuthMode.GOOGLE, result.authMode());
            verify(userPersistence).saveUser(any(User.class), isNull());
        }

        @Test
        void migrateToOAuth2_ShouldPreserveOtherUserFields() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = new User(
                    userId,
                    "testuser",
                    "John",
                    "Doe",
                    "test@example.com",
                    UserRole.MEMBER,
                    UserStatus.ACTIVE,
                    AuthMode.LOCAL,
                    Instant.now(),
                    Instant.now()
            );

            when(userQueryService.getUserById(userId)).thenReturn(existingUser);
            when(userPersistence.saveUser(any(User.class), isNull())).thenAnswer(i -> i.getArgument(0));

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
            assertEquals(AuthMode.GOOGLE, result.authMode());
        }

        @Test
        void migrateToOAuth2_ShouldThrow_WhenUserNotFound() {
            // Given
            var unknownId = UUID.randomUUID();
            when(userQueryService.getUserById(unknownId))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userUpdateService.migrateToOAuth2(unknownId, AuthMode.GOOGLE)
            );

            verify(userPersistence, never()).saveUser(any(), any());
        }
    }
}