package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.presentation.mapper.rest.BankInfoDTOMapper;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.mapper.rest.WalletDTOMapper;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.*;
import fr.uge.forkeat.service.model.wallet.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserDTOMapperTest {

  User testUser = new User(UUID.randomUUID(), "chef_arnaud", "Arnaud", "Carayol", "arnaud@test.com", UserRole.MEMBER,
          UserStatus.ACTIVE, AuthMode.LOCAL, Instant.now(), Instant.now(), false);

  @BeforeEach
  void setUp() {
  }

  @Test
  void toDTO_shouldConvertUserToDTO() {
    var dto = UserDTOMapper.toDTO(testUser);

    assertNotNull(dto);
    assertEquals("chef_arnaud", dto.username());
    assertEquals("Arnaud", dto.firstName());
    assertEquals("Carayol", dto.lastName());
    assertEquals("arnaud@test.com", dto.email());
    assertEquals("MEMBER", dto.role());
    assertEquals("ACTIVE", dto.status());
    assertEquals("LOCAL", dto.authMode());

  }

  @Test
  void toDTO_shouldConvertAllRolesCorrectly() {
    for (UserRole role : UserRole.values()) {
      var user = createUserWithRole(role);
      var dto = UserDTOMapper.toDTO(user);
      assertEquals(role.name(), dto.role());
    }
  }

  @Test
  void toDTO_shouldConvertAllStatusesCorrectly() {
    for (UserStatus status : UserStatus.values()) {
      var user = createUserWithStatus(status);
      var dto = UserDTOMapper.toDTO(user);
      assertEquals(status.name(), dto.status());
    }
  }

  @Test
  void toDTO_shouldConvertAllAuthModesCorrectly() {
    for (AuthMode authMode : AuthMode.values()) {
      var user = createUserWithAuthMode(authMode);
      var dto = UserDTOMapper.toDTO(user);
      assertEquals(authMode.name(), dto.authMode());
    }
  }

  @Test
  void toWalletDTO_shouldConvertWallet() {
    var id = UUID.randomUUID();
    var now = Instant.now();
    var wallet = new Wallet(id, testUser.id(), 2500L, now); // Fixed userId argument

    var dto = WalletDTOMapper.toDTO(wallet);

    assertNotNull(dto);
    assertEquals(id, dto.id());
    assertEquals(2500L, dto.balance());
    assertEquals(now, dto.updatedAt());
  }


  @Test
  void toWalletDTO_shouldPreserveZeroBalance() {
    var wallet = new Wallet(UUID.randomUUID(),testUser.id(), 0L, Instant.now());
    var dto = WalletDTOMapper.toDTO(wallet);
    assertEquals(0L, dto.balance());
  }

  // Removed all previous toBankInfoDTO tests as they are obsolete
  // New test for BankInfo
  @Test
  void toBankInfoResponseDTO_shouldConvertBankInfo() {
    var externalAccountId = "ext_acct_test123";
    var bankInfo = new BankInfo(testUser.id(), "Société Générale", externalAccountId);

    var dto = BankInfoDTOMapper.toResponseDTO(bankInfo); // Changed method call

    assertNotNull(dto);
    assertEquals(testUser.id(), dto.userId());
    assertEquals("Société Générale", dto.bankName());
    assertEquals(externalAccountId, dto.externalAccountId());
  }

  @Test
  void toDTO_fullUserWithAllFields() {
    var userId = UUID.randomUUID();
    var walletId = UUID.randomUUID();
    var createdAt = Instant.parse("2024-01-15T10:30:00Z");
    var updatedAt = Instant.parse("2024-06-20T14:45:00Z");
    var walletUpdatedAt = Instant.parse("2024-06-19T09:00:00Z");

    var wallet = new Wallet(walletId, userId, 15000L, walletUpdatedAt);
    var bankInfo = new BankInfo(userId, "Caisse d'Épargne", "ext_acct_ce123"); // Updated BankInfo constructor

    var user = new User(userId, "admin_user", "Jean", "Dupont", "jean.dupont@example.com", UserRole.ADMIN,
            UserStatus.ACTIVE, AuthMode.GOOGLE, createdAt, updatedAt, false);

    var dto = UserDTOMapper.toDTO(user);

    assertEquals("admin_user", dto.username());
    assertEquals("Jean", dto.firstName());
    assertEquals("Dupont", dto.lastName());
    assertEquals("jean.dupont@example.com", dto.email());
    assertEquals("ADMIN", dto.role());
    assertEquals("ACTIVE", dto.status());
    assertEquals("GOOGLE", dto.authMode());
    assertEquals(createdAt, dto.createdAt());
    assertEquals(updatedAt, dto.updatedAt());
  }

  @Test
  void toDTO_shouldConvertSuspendedUser() {
    var user = createUserWithStatus(UserStatus.SUSPENDED);
    var dto = UserDTOMapper.toDTO(user);

    assertEquals("SUSPENDED", dto.status());
  }

  @Test
  void toDTO_shouldConvertBannedUser() {
    var user = createUserWithStatus(UserStatus.BANNED);
    var dto = UserDTOMapper.toDTO(user);

    assertEquals("BANNED", dto.status());
  }

  @Test
  void toDTO_shouldConvertModeratorRole() {
    var user = createUserWithRole(UserRole.MODERATOR);
    var dto = UserDTOMapper.toDTO(user);

    assertEquals("MODERATOR", dto.role());
  }

  // Removed createUserWithBankInfoAndWallet as it's not directly used for tests after refactoring

  private User createUserWithRole(UserRole role) {
    return new User(UUID.randomUUID(), "test_user", "Test", "User", "test@test.com", role,
            UserStatus.ACTIVE, AuthMode.LOCAL, Instant.now(), Instant.now(), false);
  }

  private User createUserWithStatus(UserStatus status) {
    return new User(UUID.randomUUID(), "test_user", "Test", "User", "test@test.com", UserRole.MEMBER, status,
            AuthMode.LOCAL, Instant.now(), Instant.now(), false);
  }

  private User createUserWithAuthMode(AuthMode authMode) {
    return new User(UUID.randomUUID(), "test_user", "Test", "User", "test@test.com", UserRole.MEMBER,
            UserStatus.ACTIVE, authMode, Instant.now(), Instant.now(), false);
  }
}