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

    private User createGoogleTestUser(UUID id, String username, String email) {
        return new User(
                id,
                username,
                "John",
                "Doe",
                email,
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.GOOGLE,
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

            when(userQueryService.getUserByUsername("oldusername")).thenReturn(existingUser);
            when(userPersistence.existsByUsername("newusername")).thenReturn(false);
            when(userPersistence.updateUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
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

            // When
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

            when(userQueryService.getUserByUsername("oldusername")).thenReturn(existingUser);
            when(userPersistence.existsByUsername("takenusername")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updateProfile("oldusername", "takenusername", "First", "Last")
            );

            assertEquals("Ce nom d'utilisateur est déjà pris", exception.getMessage());
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
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");
            when(passwordEncoder.matches("correctPassword", "storedHash")).thenReturn(true);

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
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");
            when(passwordEncoder.matches("wrongPassword", "storedHash")).thenReturn(false);

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
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");
            when(passwordEncoder.matches("correctPassword", "storedHash")).thenReturn(true);
            when(userPersistence.existsByEmail("taken@example.com")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updateEmail("testuser", "taken@example.com", "correctPassword")
            );

            assertEquals("Cet email est déjà utilisé", exception.getMessage());
            verify(userPersistence, never()).updateUser(any());
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
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");

            when(passwordEncoder.matches("currentPassword", "storedHash")).thenReturn(true);
            when(passwordEncoder.matches("newPassword123", "storedHash")).thenReturn(false);
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

            // When
            userUpdateService.updatePassword("testuser", "currentPassword", "newPassword123");

            // Then
            verify(userPersistence).saveUser(any(User.class), eq("encodedNewPassword"));
        }

        @Test
        void updatePassword_ShouldThrow_WhenCurrentPasswordIncorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");

            when(passwordEncoder.matches("wrongPassword", "storedHash")).thenReturn(false);

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
            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updatePassword("testuser", "currentPassword", "short")
            );

            assertEquals("New password must be at least 8 characters", exception.getMessage());

            verifyNoInteractions(userQueryService);
            verifyNoInteractions(userPersistence);
        }

        @Test
        void updatePassword_ShouldThrow_WhenPasswordsAreSame() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userQueryService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");
            when(passwordEncoder.matches("currentPassword", "storedHash")).thenReturn(true);
            when(passwordEncoder.matches("samePassword", "storedHash")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.updatePassword("testuser", "currentPassword", "samePassword")
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
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");

            when(passwordEncoder.matches("current8", "storedHash")).thenReturn(true);
            when(passwordEncoder.matches("newpass8", "storedHash")).thenReturn(false);
            when(passwordEncoder.encode("newpass8")).thenReturn("encodedNew");

            // When
            userUpdateService.updatePassword("testuser", "current8", "newpass8");

            // Then
            verify(userPersistence).saveUser(any(User.class), eq("encodedNew"));
        }
    }

    @Nested
    class SetPasswordForOAuthUserTests {

        @Test
        void setPasswordForOAuthUser_ShouldSetPasswordAndSwitchToLocal() {
            // Given
            var userId = UUID.randomUUID();
            var googleUser = createGoogleTestUser(userId, "googleuser", "google@example.com");

            when(userQueryService.getUserByUsername("googleuser")).thenReturn(googleUser);
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPassword");

            // When
            userUpdateService.setPasswordForOAuthUser("googleuser", "newPassword123");

            // Then
            verify(userPersistence).saveUser(argThat(user ->
                    user.authMode() == AuthMode.LOCAL &&
                    user.username().equals("googleuser")
            ), eq("encodedPassword"));
        }

        @Test
        void setPasswordForOAuthUser_ShouldThrow_WhenPasswordTooShort() {
            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.setPasswordForOAuthUser("googleuser", "short")
            );

            assertEquals("New password must be at least 8 characters", exception.getMessage());
            verifyNoInteractions(userQueryService);
            verifyNoInteractions(userPersistence);
        }

        @Test
        void setPasswordForOAuthUser_ShouldThrow_WhenUserAlreadyLocal() {
            // Given
            var userId = UUID.randomUUID();
            var localUser = createTestUser(userId, "localuser", "local@example.com");

            when(userQueryService.getUserByUsername("localuser")).thenReturn(localUser);

            // When/Then
            CheckProfileUpdateFailure exception = assertThrows(
                    CheckProfileUpdateFailure.class,
                    () -> userUpdateService.setPasswordForOAuthUser("localuser", "newPassword123")
            );

            assertEquals("This user already has a local password", exception.getMessage());
            verify(userPersistence, never()).saveUser(any(), anyString());
        }

        @Test
        void setPasswordForOAuthUser_ShouldThrow_WhenUserNotFound() {
            // Given
            when(userQueryService.getUserByUsername("unknown"))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userUpdateService.setPasswordForOAuthUser("unknown", "newPassword123")
            );

            verify(userPersistence, never()).saveUser(any(), anyString());
        }
    }
}