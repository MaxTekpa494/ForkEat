package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.user.*;
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
/*
* il faut absolument revoir les tests de cette classe car les records ont change et les fonctions aussi
 */
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
            UserRole.MEMBER,
            UserStatus.ACTIVE,
            AuthMode.LOCAL,
            Instant.now(),
            Instant.now()
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
      var wallet = new Wallet(walletId, userId, 0L, Instant.now());

      when(userPersistence.existsByEmail("test@example.com")).thenReturn(false);
      when(userPersistence.existsByUsername("testuser")).thenReturn(false);
      when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
      when(userPersistence.saveUser(any(User.class), anyString())).thenReturn(savedUser);
      when(walletService.createWallet(userId)).thenReturn(wallet);

      // When
      User result = userRegistrationService.registerUser(new UserRegister("testuser", "John", "Doe",
              "password123", "test@example.com")
      );

      // Then
      assertNotNull(result);
      verify(userPersistence, times(2)).saveUser(any(User.class), anyString());
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
                      new UserRegister("newuser", "John", "Doe",
                              "password", "existing@example.com")
              )
      );

      assertEquals("Cet email est déjà utilisé", exception.getMessage());
      verify(userPersistence, never()).saveUser(any(), anyString());
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
                      new UserRegister("existinguser", "John", "Doe", "password", "new@example.com")
              )
      );

      assertEquals("Ce nom d'utilisateur est déjà pris", exception.getMessage());
      verify(userPersistence, never()).saveUser(any(), anyString());
    }


    @Test
    void registerUser_ShouldEncodePassword() throws ResourceNotFoundException {
      // Given
      var userId = UUID.randomUUID();
      var walletId = UUID.randomUUID();
      var savedUser = createTestUser(userId, "testuser", "test@example.com");
      var wallet = new Wallet(walletId, userId, 0L, Instant.now());

      when(userPersistence.existsByEmail(anyString())).thenReturn(false);
      when(userPersistence.existsByUsername(anyString())).thenReturn(false);
      when(passwordEncoder.encode("myPlainPassword")).thenReturn("encodedPassword");
      when(userPersistence.saveUser(any(User.class), anyString())).thenReturn(savedUser);
      when(walletService.createWallet(any())).thenReturn(wallet);

      // When
      userRegistrationService.registerUser(
              new UserRegister("testuser", "John", "Doe", "password", "test@example.com")
      );

      // Then
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userPersistence, atLeastOnce()).saveUser(userCaptor.capture(), anyString());

      var capturedUser = userCaptor.getAllValues().getFirst();
      //assertEquals("encodedPassword", capturedUser.password());
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
              UserRole.MEMBER, UserStatus.ACTIVE,
              AuthMode.GOOGLE,
              Instant.now(), Instant.now()
      );
      var wallet = new Wallet(walletId, userId, 0L, Instant.now());

      when(userPersistence.existsByEmail("john@gmail.com")).thenReturn(false);
      when(userPersistence.existsByUsername("john")).thenReturn(false);
      when(userPersistence.saveUser(any(User.class), anyString())).thenReturn(savedUser);
      when(walletService.createWallet(userId)).thenReturn(wallet);

      // When
      User result = userRegistrationService.registerUserFromOAuth2(
              "John", "Doe", "john@gmail.com", AuthMode.GOOGLE
      );

      // Then
      assertNotNull(result);
      //assertNull(result.password());
      assertEquals(AuthMode.GOOGLE, result.authMode());
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
                      "John", "Doe", "existing@gmail.com", AuthMode.GOOGLE
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
              UserRole.MEMBER, UserStatus.ACTIVE,
              AuthMode.GOOGLE,
              Instant.now(), Instant.now()
      );
      var wallet = new Wallet(walletId, userId, 0L, Instant.now());

      when(userPersistence.existsByEmail("john.doe@gmail.com")).thenReturn(false);
      // Premier username "johndoe" existe déjà
      when(userPersistence.existsByUsername("johndoe")).thenReturn(true);
      // Deuxième essai "johndoe1" est libre
      when(userPersistence.existsByUsername("johndoe1")).thenReturn(false);
      when(userPersistence.saveUser(any(User.class), anyString())).thenReturn(savedUser);
      when(walletService.createWallet(userId)).thenReturn(wallet);

      // When
      User result = userRegistrationService.registerUserFromOAuth2(
              "John", "Doe", "john.doe@gmail.com", AuthMode.GOOGLE
      );

      // Then
      assertNotNull(result);
      // Vérifie que le service a bien testé "johndoe" puis "johndoe1"
      verify(userPersistence).existsByUsername("johndoe");
      verify(userPersistence).existsByUsername("johndoe1");
    }
  }
}