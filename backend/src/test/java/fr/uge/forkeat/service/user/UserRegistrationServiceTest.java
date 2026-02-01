package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.junit.jupiter.api.BeforeEach;
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
class UserRegistrationServiceTest {

    @Mock
    private UserPersistence userPersistence;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

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
    class RegisterUserTests {

        @Test
        void registerUser_ShouldCreateUserWithWallet_WhenValidData() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var walletId = UUID.randomUUID();
            var savedUser = createTestUser(userId, "testuser", "test@example.com");
            var wallet = new Wallet(walletId, 0L, userId, Instant.now());

            when(userPersistence.existsByEmail("test@example.com")).thenReturn(false);
            when(userPersistence.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
            when(userPersistence.saveUser(any(User.class))).thenReturn(savedUser);
            when(walletService.createWallet(userId)).thenReturn(wallet);

            // When
            User result = userRegistrationService.registerUser(
                    "John", "Doe", "testuser", "test@example.com", "password123", UserRole.MEMBER
            );

            // Then
            assertNotNull(result);
            verify(userPersistence, times(2)).saveUser(any(User.class));
            verify(walletService).createWallet(userId);
        }

        @Test
        void registerUser_ShouldThrow_WhenEmailAlreadyExists() {
            // Given
            when(userPersistence.existsByEmail("existing@example.com")).thenReturn(true);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userRegistrationService.registerUser(
                            "John", "Doe", "newuser", "existing@example.com", "password", UserRole.MEMBER
                    )
            );

            assertEquals("Cet email est déjà utilisé", exception.getMessage());
            verify(userPersistence, never()).saveUser(any());
        }

        @Test
        void registerUser_ShouldThrow_WhenUsernameAlreadyExists() {
            // Given
            when(userPersistence.existsByEmail("new@example.com")).thenReturn(false);
            when(userPersistence.existsByUsername("existinguser")).thenReturn(true);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userRegistrationService.registerUser(
                            "John", "Doe", "existinguser", "new@example.com", "password", UserRole.MEMBER
                    )
            );

            assertEquals("Ce nom d'utilisateur est déjà pris", exception.getMessage());
            verify(userPersistence, never()).saveUser(any());
        }

        @Test
        void registerUser_ShouldEncodePassword() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var walletId = UUID.randomUUID();
            var savedUser = createTestUser(userId, "testuser", "test@example.com");
            var wallet = new Wallet(walletId, 0L, userId, Instant.now());

            when(userPersistence.existsByEmail(anyString())).thenReturn(false);
            when(userPersistence.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode("myPlainPassword")).thenReturn("encodedPassword");
            when(userPersistence.saveUser(any(User.class))).thenReturn(savedUser);
            when(walletService.createWallet(any())).thenReturn(wallet);

            // When
            userRegistrationService.registerUser(
                    "John", "Doe", "testuser", "test@example.com", "myPlainPassword", UserRole.MEMBER
            );

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userPersistence, atLeastOnce()).saveUser(userCaptor.capture());
            
            User capturedUser = userCaptor.getAllValues().get(0);
            assertEquals("encodedPassword", capturedUser.password());
        }
    }

    @Nested
    class OAuth2Tests {

        @Test
        void registerUserFromOAuth2_ShouldCreateUserWithoutPassword() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var walletId = UUID.randomUUID();

            var savedUser = new User(
                    userId, "john", "John", "Doe", "john@gmail.com",
                    null, Instant.now(), UserRole.MEMBER, UserStatus.ACTIVE,
                    AuthMode.GOOGLE, null
            );
            var wallet = new Wallet(walletId, 0L, userId, Instant.now());

            when(userPersistence.existsByEmail("john@gmail.com")).thenReturn(false);
            when(userPersistence.existsByUsername("john")).thenReturn(false);
            when(userPersistence.saveUser(any(User.class))).thenReturn(savedUser);
            when(walletService.createWallet(userId)).thenReturn(wallet);

            // When
            User result = userRegistrationService.registerUserFromOAuth2(
                    "John", "Doe", "john@gmail.com", AuthMode.GOOGLE, null
            );

            // Then
            assertNotNull(result);
            assertNull(result.password());
            assertEquals(AuthMode.GOOGLE, result.authentificationMode());
            verify(walletService).createWallet(userId);
        }

        @Test
        void registerUserFromOAuth2_ShouldThrow_WhenEmailAlreadyExists() {
            // Given
            when(userPersistence.existsByEmail("existing@gmail.com")).thenReturn(true);

            // When/Then
            assertThrows(
                    IllegalArgumentException.class,
                    () -> userRegistrationService.registerUserFromOAuth2(
                            "John", "Doe", "existing@gmail.com", AuthMode.GOOGLE, null
                    )
            );
        }

        @Test
        void registerUserFromOAuth2_ShouldGenerateUniqueUsername() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var walletId = UUID.randomUUID();

            var savedUser = new User(
                    userId, "johndoe1", "John", "Doe", "john.doe@gmail.com",
                    null, Instant.now(), UserRole.MEMBER, UserStatus.ACTIVE,
                    AuthMode.GOOGLE, null
            );
            var wallet = new Wallet(walletId, 0L, userId, Instant.now());

            when(userPersistence.existsByEmail("john.doe@gmail.com")).thenReturn(false);
            // Premier username "johndoe" existe déjà
            when(userPersistence.existsByUsername("johndoe")).thenReturn(true);
            // Deuxième essai "johndoe1" est libre
            when(userPersistence.existsByUsername("johndoe1")).thenReturn(false);
            when(userPersistence.saveUser(any(User.class))).thenReturn(savedUser);
            when(walletService.createWallet(userId)).thenReturn(wallet);

            // When
            User result = userRegistrationService.registerUserFromOAuth2(
                    "John", "Doe", "john.doe@gmail.com", AuthMode.GOOGLE, null
            );

            // Then
            assertNotNull(result);
            // Vérifie que le service a bien testé "johndoe" puis "johndoe1"
            verify(userPersistence).existsByUsername("johndoe");
            verify(userPersistence).existsByUsername("johndoe1");
        }
    }
}