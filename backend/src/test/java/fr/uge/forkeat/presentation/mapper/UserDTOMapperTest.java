package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserDTOMapperTest {

    private UserDTOMapper mapper;

    @BeforeEach
    void setUp() {
        //mapper = new UserDTOMapper();
    }

    @Test
    void toDTO_shouldConvertUserToDTO() {
        var id = UUID.randomUUID();
        var walletId = UUID.randomUUID();
        var now = Instant.now();

        var wallet = new Wallet(walletId, 1000L, now);
        var bankInfo = new BankInfo("BNP Paribas", "FR7612345678901234567890123", "BNPAFRPP");
        var user = new User(
                id,
                "chef_arnaud",
                "Arnaud",
                "Carayol",
                "arnaud@test.com",
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                bankInfo,
                wallet,
                now,
                now
        );

        var dto = mapper.toDTO(user);

        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals("chef_arnaud", dto.username());
        assertEquals("Arnaud", dto.firstName());
        assertEquals("Carayol", dto.lastName());
        assertEquals("arnaud@test.com", dto.email());
        assertEquals("MEMBER", dto.role());
        assertEquals("ACTIVE", dto.status());
        assertEquals("LOCAL", dto.authMode());
        assertNotNull(dto.bankInfo());
        assertNotNull(dto.wallet());
    }

    @Test
    void toDTO_shouldReturnNullWhenUserIsNull() {
        var dto = UserDTOMapper.toDTO(null);
        assertNull(dto);
    }

    @Test
    void toDTO_shouldHandleNullBankInfo() {
        var user = createUserWithBankInfoAndWallet(null, new Wallet(UUID.randomUUID(), 500L, Instant.now()));

        var dto = UserDTOMapper.toDTO(user);

        assertNotNull(dto);
        assertNull(dto.bankInfo());
    }

    @Test
    void toDTO_shouldHandleNullWallet() {
        var bankInfo = new BankInfo("Crédit Agricole", "FR7698765432109876543210987", "AGRIFRPP");
        var user = createUserWithBankInfoAndWallet(bankInfo, null);

        var dto = UserDTOMapper.toDTO(user);

        assertNotNull(dto);
        assertNull(dto.wallet());
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
        var wallet = new Wallet(id, 2500L, now);

        var dto = UserDTOMapper.toWalletDTO(wallet);

        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals(2500L, dto.balance());
        assertEquals(now, dto.updatedAt());
    }

    @Test
    void toWalletDTO_shouldReturnNullWhenWalletIsNull() {
        var dto = mapper.toWalletDTO(null);
        assertNull(dto);
    }

    @Test
    void toWalletDTO_shouldPreserveZeroBalance() {
        var wallet = new Wallet(UUID.randomUUID(), 0L, Instant.now());
        var dto = mapper.toWalletDTO(wallet);
        assertEquals(0L, dto.balance());
    }

    @Test
    void toBankInfoDTO_shouldConvertBankInfo() {
        var bankInfo = new BankInfo("Société Générale", "FR7611111222223333344444555", "SOGEFRPP");

        var dto = mapper.toBankInfoDTO(bankInfo);

        assertNotNull(dto);
        assertEquals("Société Générale", dto.bankName());
        assertEquals("SOGEFRPP", dto.bic());
    }

    @Test
    void toBankInfoDTO_shouldReturnNullWhenBankInfoIsNull() {
        var dto = mapper.toBankInfoDTO(null);
        assertNull(dto);
    }

    @Test
    void toBankInfoDTO_shouldMaskIbanCorrectly() {
        var bankInfo = new BankInfo("BNP", "FR7612345678901234567890123", "BNPAFRPP");

        var dto = mapper.toBankInfoDTO(bankInfo);

        assertEquals("FR76****0123", dto.maskedIban());
    }

    @Test
    void toBankInfoDTO_shouldMaskShortIban() {
        var bankInfo = new BankInfo("Test Bank", "FR761234", "TESTFRPP");

        var dto = mapper.toBankInfoDTO(bankInfo);

        assertEquals("****", dto.maskedIban());
    }

    @Test
    void toBankInfoDTO_shouldMaskExactly8CharacterIban() {
        var bankInfo = new BankInfo("Test Bank", "12345678", "TESTFRPP");

        var dto = mapper.toBankInfoDTO(bankInfo);

        assertEquals("****", dto.maskedIban());
    }

    @Test
    void toBankInfoDTO_shouldMaskIbanWith9Characters() {
        var bankInfo = new BankInfo("Test Bank", "123456789", "TESTFRPP");

        var dto = mapper.toBankInfoDTO(bankInfo);

        assertEquals("1234****6789", dto.maskedIban());
    }

    @Test
    void toBankInfoDTO_shouldHandleNullIban() {
        // Note: BankInfo requiert iban non null, donc ce cas ne devrait pas arriver
        // mais le mapper le gère gracieusement via le constructeur
        assertThrows(NullPointerException.class, () ->
                new BankInfo("Test Bank", null, "TESTFRPP")
        );
    }

    @Test
    void toDTO_fullUserWithAllFields() {
        var userId = UUID.randomUUID();
        var walletId = UUID.randomUUID();
        var createdAt = Instant.parse("2024-01-15T10:30:00Z");
        var updatedAt = Instant.parse("2024-06-20T14:45:00Z");
        var walletUpdatedAt = Instant.parse("2024-06-19T09:00:00Z");

        var wallet = new Wallet(walletId, 15000L, walletUpdatedAt);
        var bankInfo = new BankInfo("Caisse d'Épargne", "FR7612345678901234567890189", "CEPAFRPP");

        var user = new User(
                userId,
                "admin_user",
                "Jean",
                "Dupont",
                "jean.dupont@example.com",
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                AuthMode.GOOGLE,
                bankInfo,
                wallet,
                createdAt,
                updatedAt
        );

        var dto = mapper.toDTO(user);

        assertEquals(userId, dto.id());
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
        assertEquals(walletId, dto.wallet().id());
        assertEquals(15000L, dto.wallet().balance());
        assertEquals(walletUpdatedAt, dto.wallet().updatedAt());

        // BankInfo
        assertEquals("Caisse d'Épargne", dto.bankInfo().bankName());
        assertEquals("FR76****0189", dto.bankInfo().maskedIban());
        assertEquals("CEPAFRPP", dto.bankInfo().bic());
    }

    @Test
    void toDTO_shouldConvertSuspendedUser() {
        var user = createUserWithStatus(UserStatus.SUSPENDED);
        var dto = mapper.toDTO(user);

        assertEquals("SUSPENDED", dto.status());
    }

    @Test
    void toDTO_shouldConvertBannedUser() {
        var user = createUserWithStatus(UserStatus.BANNED);
        var dto = mapper.toDTO(user);

        assertEquals("BANNED", dto.status());
    }

    @Test
    void toDTO_shouldConvertModeratorRole() {
        var user = createUserWithRole(UserRole.MODERATOR);
        var dto = mapper.toDTO(user);

        assertEquals("MODERATOR", dto.role());
    }

    private User createUserWithBankInfoAndWallet(BankInfo bankInfo, Wallet wallet) {
        return new User(
                UUID.randomUUID(),
                "test_user",
                "Test",
                "User",
                "test@test.com",
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                bankInfo,
                wallet,
                Instant.now(),
                Instant.now()
        );
    }

    private User createUserWithRole(UserRole role) {
        return new User(
                UUID.randomUUID(),
                "test_user",
                "Test",
                "User",
                "test@test.com",
                role,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    private User createUserWithStatus(UserStatus status) {
        return new User(
                UUID.randomUUID(),
                "test_user",
                "Test",
                "User",
                "test@test.com",
                UserRole.MEMBER,
                status,
                AuthMode.LOCAL,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    private User createUserWithAuthMode(AuthMode authMode) {
        return new User(
                UUID.randomUUID(),
                "test_user",
                "Test",
                "User",
                "test@test.com",
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                authMode,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }
}
