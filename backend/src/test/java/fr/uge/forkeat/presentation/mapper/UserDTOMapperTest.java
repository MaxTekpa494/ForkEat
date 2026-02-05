package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.presentation.mapper.rest.BankInfoDTOMapper;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.mapper.rest.WalletDTOMapper;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserDTOMapperTest {

  User testUser = new User(UUID.randomUUID(), "chef_arnaud", "Arnaud", "Carayol", "arnaud@test.com", UserRole.MEMBER,
          UserStatus.ACTIVE, AuthMode.LOCAL, Instant.now(), Instant.now());

  @BeforeEach
  void setUp() {
  }

  @Test
  void toDTO_shouldConvertUserToDTO() {
    var walletId = UUID.randomUUID();
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
    var wallet = new Wallet(id, id, 2500L, now);

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

  @Test
  void toBankInfoDTO_shouldConvertBankInfo() {
    var bankInfo = new BankInfo(testUser.id(), "Société Générale", "FR7611111222223333344444555", "SOGEFRPP");

    var dto = BankInfoDTOMapper.toDTO(bankInfo);

    assertNotNull(dto);
    assertEquals("Société Générale", dto.bankName());
    assertEquals("SOGEFRPP", dto.bic());
  }

  @Test
  void toBankInfoDTO_shouldMaskIbanCorrectly() {
    var bankInfo = new BankInfo(testUser.id(), "BNP", "FR7612345678901234567890123", "BNPAFRPP");

    var dto = BankInfoDTOMapper.toDTO(bankInfo);

    assertEquals("FR76****0123", dto.maskedIban());
  }

  @Test
  void toBankInfoDTO_shouldMaskShortIban() {
    var bankInfo = new BankInfo(testUser.id(), "Test Bank", "FR761234", "TESTFRPP");

    var dto = BankInfoDTOMapper.toDTO(bankInfo);

    assertEquals("****", dto.maskedIban());
  }

  @Test
  void toBankInfoDTO_shouldMaskExactly8CharacterIban() {
    var bankInfo = new BankInfo(testUser.id(), "Test Bank", "12345678", "TESTFRPP");

    var dto = BankInfoDTOMapper.toDTO(bankInfo);

    assertEquals("****", dto.maskedIban());
  }

  @Test
  void toBankInfoDTO_shouldMaskIbanWith9Characters() {
    var bankInfo = new BankInfo(testUser.id(), "Test Bank", "123456789", "TESTFRPP");

    var dto = BankInfoDTOMapper.toDTO(bankInfo);

    assertEquals("1234****6789", dto.maskedIban());
  }

  @Test
  void toBankInfoDTO_shouldHandleNullIban() {
    // Note: BankInfo requiert iban non null, donc ce cas ne devrait pas arriver
    // mais le mapper le gère gracieusement via le constructeur
    assertThrows(NullPointerException.class, () -> new BankInfo(UUID.randomUUID(), "Test Bank", null, "TESTFRPP"));
  }

  @Test
  void toDTO_fullUserWithAllFields() {
    var userId = UUID.randomUUID();
    var walletId = UUID.randomUUID();
    var createdAt = Instant.parse("2024-01-15T10:30:00Z");
    var updatedAt = Instant.parse("2024-06-20T14:45:00Z");
    var walletUpdatedAt = Instant.parse("2024-06-19T09:00:00Z");

    var wallet = new Wallet(walletId, userId, 15000L, walletUpdatedAt);
    var bankInfo = new BankInfo(userId, "Caisse d'Épargne", "FR7612345678901234567890189", "CEPAFRPP");

    var user = new User(userId, "admin_user", "Jean", "Dupont", "jean.dupont@example.com", UserRole.ADMIN,
            UserStatus.ACTIVE, AuthMode.GOOGLE, createdAt, updatedAt);

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

    // Wallet

    var bankInfoDTO = BankInfoDTOMapper.toDTO(bankInfo);
    // BankInfo
    assertEquals("Caisse d'Épargne", bankInfoDTO.bankName());
    assertEquals("FR76****0189", bankInfoDTO.maskedIban());
    assertEquals("CEPAFRPP", bankInfoDTO.bic());
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

  private User createUserWithBankInfoAndWallet(BankInfo bankInfo, Wallet wallet) {
    return new User(UUID.randomUUID(), "test_user", "Test", "User", "test@test.com", UserRole.MEMBER,
            UserStatus.ACTIVE, AuthMode.LOCAL, Instant.now(), Instant.now());
  }

  private User createUserWithRole(UserRole role) {
    return new User(UUID.randomUUID(), "test_user", "Test", "User", "test@test.com", role, UserStatus.ACTIVE,
            AuthMode.LOCAL, Instant.now(), Instant.now());
  }

  private User createUserWithStatus(UserStatus status) {
    return new User(UUID.randomUUID(), "test_user", "Test", "User", "test@test.com", UserRole.MEMBER, status,
            AuthMode.LOCAL, Instant.now(), Instant.now());
  }

  private User createUserWithAuthMode(AuthMode authMode) {
    return new User(UUID.randomUUID(), "test_user", "Test", "User", "test@test.com", UserRole.MEMBER,
            UserStatus.ACTIVE, authMode, Instant.now(), Instant.now());
  }
}
