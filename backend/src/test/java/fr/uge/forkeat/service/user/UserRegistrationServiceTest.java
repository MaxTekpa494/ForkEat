package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.RegisterFailureException;
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
import fr.uge.forkeat.service.port.PasswordHasher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

  @Mock
  private UserPersistence userPersistence;

  @Mock
  private PasswordHasher passwordHasher;

  @Mock
  private WalletService walletService;

  @Mock
  private EmailVerificationService emailVerificationService;

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
            Instant.now(),
            false
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
      when(passwordHasher.hash("Password123")).thenReturn("hashedPassword123");
      when(userPersistence.saveUser(any(User.class), anyString())).thenReturn(savedUser);
      when(walletService.createWallet(userId)).thenReturn(wallet);

      doNothing().when(emailVerificationService).sendEmailConfirmation(any(UUID.class), anyString());

      User result = userRegistrationService.registerUser(
              new UserRegister("testuser", "John", "Doe", "Password123", "test@example.com")
      );

      assertNotNull(result);
      verify(userPersistence, times(1)).saveUser(any(User.class), anyString());
      verify(walletService).createWallet(userId);
      verify(emailVerificationService).sendEmailConfirmation(userId, "test@example.com");
    }

    @Test
    void registerUser_ShouldThrow_WhenEmailAlreadyExists() {
      when(userPersistence.existsByEmail("existing@example.com")).thenReturn(true);

      RegisterFailureException exception = assertThrows(
              RegisterFailureException.class,
              () -> userRegistrationService.registerUser(
                      new UserRegister("newuser", "John", "Doe", "password", "existing@example.com")
              )
      );

      assertEquals("This email is already in use", exception.getMessage());
      verify(userPersistence, never()).saveUser(any(), anyString());
      verify(emailVerificationService, never()).sendEmailConfirmation(any(), anyString());
    }

    @Test
    void registerUser_ShouldThrow_WhenUsernameAlreadyExists() {
      when(userPersistence.existsByEmail("new@example.com")).thenReturn(false);
      when(userPersistence.existsByUsername("existinguser")).thenReturn(true);

      RegisterFailureException exception = assertThrows(
              RegisterFailureException.class,
              () -> userRegistrationService.registerUser(
                      new UserRegister("existinguser", "John", "Doe", "password", "new@example.com")
              )
      );

      assertEquals("This username is already taken", exception.getMessage());
      verify(userPersistence, never()).saveUser(any(), anyString());
      verify(emailVerificationService, never()).sendEmailConfirmation(any(), anyString());
    }
  }

  @Nested
  class OAuth2Tests {

    @Test
    void registerUserFromOAuth2_ShouldCreateUserWithoutPassword() {
      var walletId = UUID.randomUUID();
      var wallet = new Wallet(walletId, UUID.randomUUID(), 0L, Instant.now());

      when(userPersistence.saveUser(any(User.class), isNull()))
              .thenAnswer(invocation -> invocation.getArgument(0));

      when(userPersistence.findByEmail("john@gmail.com")).thenReturn(Optional.empty());

      when(walletService.createWallet(any(UUID.class))).thenReturn(wallet);

      User result = userRegistrationService.registerUserFromOAuth2(
              "John", "Doe", "john@gmail.com", AuthMode.GOOGLE
      );

      assertNotNull(result);
      assertEquals(AuthMode.GOOGLE, result.authMode());

      verify(walletService).createWallet(any(UUID.class));
      verify(userPersistence).findByEmail("john@gmail.com");
      verifyNoInteractions(emailVerificationService);
    }


    @Test
    void registerUserFromOAuth2_ShouldGenerateUniqueUsername() {
      var walletId = UUID.randomUUID();
      var wallet = new Wallet(walletId, UUID.randomUUID(), 0L, Instant.now());

      when(userPersistence.saveUser(any(User.class), isNull()))
              .thenAnswer(invocation -> invocation.getArgument(0));
      when(userPersistence.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
      when(userPersistence.existsByUsername(anyString()))
              .thenReturn(true)
              .thenReturn(false);
      when(walletService.createWallet(any(UUID.class))).thenReturn(wallet);

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