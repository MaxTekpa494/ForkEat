package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.CheckProfileUpdateFailureException;
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
import fr.uge.forkeat.service.port.PasswordHasherPort;

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
    private UserService userService;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private EmailVerificationService emailVerificationService;

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
                Instant.now(),
                false
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
                Instant.now(),
                false
        );
    }

    @Nested
    class UpdateProfileTests {

        @Test
        void updateProfile_ShouldUpdateUserProfile_WhenValidData() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "oldusername", "test@example.com");

            when(userService.getUserByUsername("oldusername")).thenReturn(existingUser);
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

            verify(userService).getUserByUsername("oldusername");
            verify(userPersistence).existsByUsername("newusername");
            verify(userPersistence).updateUser(any(User.class));
        }

        @Test
        void updateProfile_ShouldNotCheckUsername_WhenUsernameNotChanged() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "sameusername", "test@example.com");

            when(userService.getUserByUsername("sameusername")).thenReturn(existingUser);
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

            when(userService.getUserByUsername("oldusername")).thenReturn(existingUser);
            when(userPersistence.existsByUsername("takenusername")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.updateProfile("oldusername", "takenusername", "First", "Last")
            );

            assertEquals("Ce nom d'utilisateur est déjà pris", exception.getMessage());
            verify(userPersistence, never()).updateUser(any());
        }

        @Test
        void updateProfile_ShouldThrow_WhenFieldIsBlank() {
            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.updateProfile("user", "", "First", "Last")
            );

            assertEquals("Veuillez remplir tous les champs", exception.getMessage());
            verifyNoInteractions(userService);
            verifyNoInteractions(userPersistence);
        }

        @Test
        void updateProfile_ShouldThrow_WhenUserNotFound() {
            // Given
            when(userService.getUserByUsername("unknown"))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userUpdateService.updateProfile("unknown", "newusername", "First", "Last")
            );
        }
    }

    @Nested
    class RequestEmailChangeTests {

        @Test
        void requestEmailChange_ShouldSendVerificationCode_WhenPasswordCorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");
            when(passwordHasherPort.matches("correctPassword", "storedHash")).thenReturn(true);

            when(userPersistence.existsByEmail("new@example.com")).thenReturn(false);

            // When
            userUpdateService.requestEmailChange("testuser", "new@example.com", "correctPassword", null, null);

            // Then
            // Verify verification email is sent with no pending password hash (LOCAL user)
            verify(emailVerificationService).sendEmailChangeCode(userId, "old@example.com", "new@example.com");
            verify(userPersistence, never()).updateUser(any(User.class));
        }

        @Test
        void requestEmailChange_ShouldHashPasswordAndSendCode_ForGoogleUser() {
            // Given
            var userId = UUID.randomUUID();
            var googleUser = createGoogleTestUser(userId, "googleuser", "old@example.com");

            when(userService.getUserByUsername("googleuser")).thenReturn(googleUser);
            when(userPersistence.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordHasherPort.hash("NewPass1!")).thenReturn("hashedNewPass");

            // When
            userUpdateService.requestEmailChange("googleuser", "new@example.com", null, "NewPass1!", "NewPass1!");

            // Then
            verify(emailVerificationService).sendEmailChangeCode(userId, "old@example.com", "new@example.com", "hashedNewPass");
            verify(userPersistence, never()).updateUser(any(User.class));
        }

        @Test
        void requestEmailChange_ShouldThrow_WhenPasswordIncorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");
            when(passwordHasherPort.matches("wrongPassword", "storedHash")).thenReturn(false);

            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.requestEmailChange("testuser", "new@example.com", "wrongPassword", null, null)
            );

            assertEquals("Incorrect password", exception.getMessage());
            verifyNoInteractions(emailVerificationService);
        }

        @Test
        void requestEmailChange_ShouldThrow_WhenNewEmailAlreadyUsed() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "old@example.com");

            when(userService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");
            when(passwordHasherPort.matches("correctPassword", "storedHash")).thenReturn(true);
            when(userPersistence.existsByEmail("taken@example.com")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.requestEmailChange("testuser", "taken@example.com", "correctPassword", null, null)
            );

            assertEquals("Cet email est déjà utilisé", exception.getMessage());
            verifyNoInteractions(emailVerificationService);
        }
    }

    @Nested
    class RequestPasswordChangeTests {

        @Test
        void requestPasswordChange_ShouldSendVerificationCode_WhenCurrentPasswordCorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");

            when(passwordHasherPort.matches("currentPassword", "storedHash")).thenReturn(true);
            when(passwordHasherPort.matches("newPassword123", "storedHash")).thenReturn(false);
            when(passwordHasherPort.hash("newPassword123")).thenReturn("hashedNewPass");

            // When
            userUpdateService.requestPasswordChange("testuser", "currentPassword", "newPassword123", "newPassword123");

            // Then
            verify(emailVerificationService).sendPasswordChangeCode(userId, "test@example.com", "hashedNewPass");
            verify(userPersistence, never()).saveUser(any(), anyString());
        }

        @Test
        void requestPasswordChange_ShouldThrow_WhenCurrentPasswordIncorrect() {
            // Given
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");

            when(passwordHasherPort.matches("wrongPassword", "storedHash")).thenReturn(false);

            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.requestPasswordChange("testuser", "wrongPassword", "newPassword123", "newPassword123")
            );

            assertEquals("Incorrect current password", exception.getMessage());
            verify(emailVerificationService, never()).sendPasswordChangeCode(any(), any(), any());
        }

        @Test
        void requestPasswordChange_ShouldThrow_WhenNewPasswordTooShort() {
            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.requestPasswordChange("testuser", "currentPassword", "short", "short")
            );

            assertEquals("Le mot de passe doit contenir au moins 8 caractères", exception.getMessage());

            verifyNoInteractions(userService);
            verifyNoInteractions(userPersistence);
            verifyNoInteractions(emailVerificationService);
        }

        @Test
        void requestPasswordChange_ShouldThrow_WhenPasswordsAreSame() {
            // Given — "SamePass1" passe PasswordValidator (longueur, majuscule, chiffre)
            var userId = UUID.randomUUID();
            var existingUser = createTestUser(userId, "testuser", "test@example.com");

            when(userService.getUserByUsername("testuser")).thenReturn(existingUser);
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("storedHash");
            when(passwordHasherPort.matches("currentPassword", "storedHash")).thenReturn(true);
            when(passwordHasherPort.matches("SamePass1", "storedHash")).thenReturn(true);

            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.requestPasswordChange("testuser", "currentPassword", "SamePass1", "SamePass1")
            );

            assertEquals("Passwords are the same", exception.getMessage());
            verify(emailVerificationService, never()).sendPasswordChangeCode(any(), any(), any());
        }
    }

    @Nested
    class SetPasswordForOAuthUserTests {

        @Test
        void setPasswordForOAuthUser_ShouldSetPasswordAndSwitchToLocal() {
            // Given
            var userId = UUID.randomUUID();
            var googleUser = createGoogleTestUser(userId, "googleuser", "google@example.com");

            when(userService.getUserByUsername("googleuser")).thenReturn(googleUser);
            when(passwordHasherPort.hash("newPassword123")).thenReturn("encodedPassword");

            // When
            userUpdateService.setPasswordForOAuthUser("googleuser", "newPassword123", "newPassword123");

            // Then
            verify(userPersistence).saveUser(argThat(user ->
                    user.authMode() == AuthMode.LOCAL &&
                            user.username().equals("googleuser")
            ), eq("encodedPassword"));
        }

        @Test
        void setPasswordForOAuthUser_ShouldThrow_WhenPasswordTooShort() {
            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.setPasswordForOAuthUser("googleuser", "short", "short")
            );

            assertEquals("Le mot de passe doit contenir au moins 8 caractères", exception.getMessage());
            verifyNoInteractions(userService);
            verifyNoInteractions(userPersistence);
        }

        @Test
        void setPasswordForOAuthUser_ShouldThrow_WhenUserAlreadyLocal() {
            // Given
            var userId = UUID.randomUUID();
            var localUser = createTestUser(userId, "localuser", "local@example.com");

            when(userService.getUserByUsername("localuser")).thenReturn(localUser);

            // When/Then
            CheckProfileUpdateFailureException exception = assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.setPasswordForOAuthUser("localuser", "newPassword123", "newPassword123")
            );

            assertEquals("This user already has a local password", exception.getMessage());
            verify(userPersistence, never()).saveUser(any(), anyString());
        }

        @Test
        void setPasswordForOAuthUser_ShouldThrow_WhenUserNotFound() {
            // Given
            when(userService.getUserByUsername("unknown"))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userUpdateService.setPasswordForOAuthUser("unknown", "newPassword123", "newPassword123")
            );

            verify(userPersistence, never()).saveUser(any(), anyString());
        }
    }

    @Nested
    class ConfirmForgotPasswordChangeTests {

        @Test
        void confirmForgotPasswordChange_ShouldResetPassword_WhenCodeValid() {
            var userId = UUID.randomUUID();
            var user = createTestUser(userId, "testuser", "test@example.com");

            when(userService.getUserByEmail("test@example.com")).thenReturn(user);
            when(passwordHasherPort.hash("NewPass1")).thenReturn("hashedNew");

            userUpdateService.confirmForgotPasswordChange("test@example.com", "123456", "NewPass1", "NewPass1");

            verify(emailVerificationService).confirmPasswordReset(userId, "123456", "hashedNew");
        }

        @Test
        void confirmForgotPasswordChange_ShouldThrow_WhenPasswordsDontMatch() {
            assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.confirmForgotPasswordChange("test@example.com", "123456", "NewPass1", "Different1")
            );
            verifyNoInteractions(userService);
            verifyNoInteractions(emailVerificationService);
        }

        @Test
        void confirmForgotPasswordChange_ShouldThrow_WhenPasswordTooShort() {
            assertThrows(
                    CheckProfileUpdateFailureException.class,
                    () -> userUpdateService.confirmForgotPasswordChange("test@example.com", "123456", "short", "short")
            );
            verifyNoInteractions(userService);
            verifyNoInteractions(emailVerificationService);
        }

        @Test
        void confirmForgotPasswordChange_ShouldThrow_WhenUserNotFound() {
            when(userService.getUserByEmail("unknown@example.com"))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userUpdateService.confirmForgotPasswordChange("unknown@example.com", "123456", "NewPass1", "NewPass1")
            );
            verify(emailVerificationService, never()).confirmPasswordReset(any(), any(), any());
        }
    }
}