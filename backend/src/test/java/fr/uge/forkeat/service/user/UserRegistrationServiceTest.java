package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.RegisterFailure;
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
import static org.mockito.ArgumentMatchers.*;
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
      var userId = UUID.randomUUID();
      var walletId = UUID.randomUUID();
      var savedUser = createTestUser(userId, "testuser", "test@example.com");
      var wallet = new Wallet(walletId, userId, 0L, Instant.now());

      when(userPersistence.existsByEmail("test@example.com")).thenReturn(false);
      when(userPersistence.existsByUsername("testuser")).thenReturn(false);
      when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
      when(userPersistence.saveUser(any(User.class), anyString())).thenReturn(savedUser);
      when(walletService.createWallet(userId)).thenReturn(wallet);

      User result = userRegistrationService.registerUser(
              new UserRegister("testuser", "John", "Doe", "password123", "test@example.com")
      );

      assertNotNull(result);
      verify(userPersistence, times(1)).saveUser(any(User.class), anyString());
      verify(walletService).createWallet(userId);
    }

    @Test
    void registerUser_ShouldThrow_WhenEmailAlreadyExists() {
      when(userPersistence.existsByEmail("existing@example.com")).thenReturn(true);

      RegisterFailure exception = assertThrows(
              RegisterFailure.class,
              () -> userRegistrationService.registerUser(
                      new UserRegister("newuser", "John", "Doe", "password", "existing@example.com")
              )
      );

      assertEquals("This email is already in use", exception.getMessage());
      verify(userPersistence, never()).saveUser(any(), anyString());
    }

    @Test
    void registerUser_ShouldThrow_WhenUsernameAlreadyExists() {
      when(userPersistence.existsByEmail("new@example.com")).thenReturn(false);
      when(userPersistence.existsByUsername("existinguser")).thenReturn(true);

      RegisterFailure exception = assertThrows(
              RegisterFailure.class,
              () -> userRegistrationService.registerUser(
                      new UserRegister("existinguser", "John", "Doe", "password", "new@example.com")
              )
      );

      assertEquals("This username is already taken", exception.getMessage());
      verify(userPersistence, never()).saveUser(any(), anyString());
    }
  }

  @Nested
  class OAuth2Tests {

    @Test
    void registerUserFromOAuth2_ShouldCreateUserWithoutPassword() throws ResourceNotFoundException {
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
      when(userPersistence.saveUser(any(User.class), isNull())).thenReturn(savedUser);
      when(walletService.createWallet(userId)).thenReturn(wallet);

      User result = userRegistrationService.registerUserFromOAuth2(
              "John", "Doe", "john@gmail.com", AuthMode.GOOGLE
      );

      assertNotNull(result);
      assertEquals(AuthMode.GOOGLE, result.authMode());
      verify(walletService).createWallet(userId);
    }

    @Test
    void registerUserFromOAuth2_ShouldThrow_WhenEmailAlreadyExists() {
      when(userPersistence.existsByEmail("existing@gmail.com")).thenReturn(true);

      assertThrows(
              RegisterFailure.class,
              () -> userRegistrationService.registerUserFromOAuth2(
                      "John", "Doe", "existing@gmail.com", AuthMode.GOOGLE
              )
      );
    }

    @Test
    void registerUserFromOAuth2_ShouldGenerateUniqueUsername() throws ResourceNotFoundException {
      var userId = UUID.randomUUID();
      var walletId = UUID.randomUUID();

      var savedUser = new User(
              userId, "johndoe", "John", "Doe", "john.doe@gmail.com",
              UserRole.MEMBER, UserStatus.ACTIVE,
              AuthMode.GOOGLE,
              Instant.now(), Instant.now()
      );
      var wallet = new Wallet(walletId, userId, 0L, Instant.now());

      when(userPersistence.existsByEmail("john.doe@gmail.com")).thenReturn(false);
      when(userPersistence.existsByUsername(anyString()))
              .thenReturn(true)
              .thenReturn(false);
      when(userPersistence.saveUser(any(User.class), isNull())).thenReturn(savedUser);
      when(walletService.createWallet(userId)).thenReturn(wallet);

      User result = userRegistrationService.registerUserFromOAuth2(
              "John", "Doe", "john.doe@gmail.com", AuthMode.GOOGLE
      );

      assertNotNull(result);

      ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
      verify(userPersistence, times(2)).existsByUsername(captor.capture());

      assertTrue(captor.getAllValues().get(0).startsWith("johndoe"));
      assertTrue(captor.getAllValues().get(1).startsWith("johndoe"));
    }
  }
}
