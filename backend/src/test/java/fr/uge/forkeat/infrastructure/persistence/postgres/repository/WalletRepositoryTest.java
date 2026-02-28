package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class WalletRepositoryTest extends AbstractIntegrationTest {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    private UserEntity savedUser;
    private WalletEntity savedWallet;

    @Autowired
    WalletRepositoryTest(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @BeforeEach
    void setUp() {
        var user = new UserEntity();
        user.setUsername("wallet_repo_user");
        user.setFirstName("Wallet");
        user.setLastName("Test");
        user.setEmail("wallet_repo@test.com");
        user.setPassword("hashed-pw");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        savedUser = userRepository.save(user);

        var wallet = new WalletEntity(2500L, savedUser);
        savedWallet = walletRepository.save(wallet);
    }

    @Test
    void shouldFindByUserId() {
        var result = walletRepository.findByUserId(savedUser.getId());

        assertTrue(result.isPresent());
        assertEquals(savedWallet.getId(), result.get().getId());
        assertEquals(2500L, result.get().getBalance());
    }

    @Test
    void shouldReturnEmptyForUnknownUserId() {
        var result = walletRepository.findByUserId(java.util.UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindBalanceByUserId() {
        var balance = walletRepository.findBalanceByUserIdReadOnly(savedUser.getId());

        assertEquals(2500L, balance);
    }

    @Test
    void shouldSaveAndRetrieveWallet() {
        var found = walletRepository.findById(savedWallet.getId());

        assertTrue(found.isPresent());
        assertEquals(2500L, found.get().getBalance());
        assertEquals(savedUser.getId(), found.get().getUser().getId());
    }

    @Test
    void shouldUpdateWalletBalance() {
        savedWallet.setBalance(5000L);
        walletRepository.save(savedWallet);

        var found = walletRepository.findById(savedWallet.getId());
        assertTrue(found.isPresent());
        assertEquals(5000L, found.get().getBalance());
    }
}
