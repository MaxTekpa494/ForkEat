package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.model.transaction.TransactionStatus; // New import
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant; // New import

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class TransactionRepositoryTest extends AbstractIntegrationTest {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    private WalletEntity savedWallet;

    @Autowired
    TransactionRepositoryTest(TransactionRepository transactionRepository,
                               WalletRepository walletRepository,
                               UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @BeforeEach
    void setUp() {
        var user = new UserEntity();
        user.setUsername("tx_repo_user");
        user.setFirstName("Tx");
        user.setLastName("Repo");
        user.setEmail("tx_repo@test.com");
        user.setPassword("hashed-pw");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        var savedUser = userRepository.save(user);

        var wallet = new WalletEntity(5000L, savedUser);
        savedWallet = walletRepository.save(wallet);
    }

    @Test
    void shouldReturnTrueWhenStripeTransactionExists() {
        // Updated TransactionEntity constructor call
        var tx = new TransactionEntity(null, savedWallet, 1000L, "stripe_unique_123",
                TransactionType.RECHARGE, TransactionStatus.SUCCEEDED, Instant.now());
        transactionRepository.save(tx);

        assertTrue(transactionRepository.existsByStripeTransactionID("stripe_unique_123"));
    }

    @Test
    void shouldReturnFalseWhenStripeTransactionDoesNotExist() {
        assertFalse(transactionRepository.existsByStripeTransactionID("nonexistent_stripe_id"));
    }

    @Test
    void shouldSaveAndRetrieveTransaction() {
        // Updated TransactionEntity constructor call
        var tx = new TransactionEntity(null, savedWallet, 2000L, "stripe_save_test",
                TransactionType.RECHARGE, TransactionStatus.SUCCEEDED, Instant.now());
        var saved = transactionRepository.save(tx);

        assertNotNull(saved.getId());
        // TransactionRepository declares JpaRepository<TransactionEntity, String>
        // but the actual ID is UUID — use existsByStripeTransactionID instead
        assertTrue(transactionRepository.existsByStripeTransactionID("stripe_save_test"));
        assertEquals(TransactionStatus.SUCCEEDED, saved.getStatus()); // New assertion
    }

    @Test
    void shouldPersistAllFields() {
        // Updated TransactionEntity constructor call
        var tx = new TransactionEntity(null, savedWallet, 3000L, "stripe_fields",
                TransactionType.RECHARGE, TransactionStatus.SUCCEEDED, Instant.now());
        var saved = transactionRepository.save(tx);

        assertEquals(3000L, saved.getAmount());
        assertEquals("stripe_fields", saved.getStripeTransactionID());
        assertEquals(TransactionType.RECHARGE, saved.getTransactionType());
        assertNotNull(saved.getDestinationWallet());
        assertEquals(TransactionStatus.SUCCEEDED, saved.getStatus()); // New assertion
    }
    
    // Test for findByStripeTransactionID
    @Test
    void shouldFindTransactionByStripeTransactionID() {
        // Given
        var stripeTxId = "stripe_find_id_test";
        var tx = new TransactionEntity(null, savedWallet, 100L, stripeTxId, TransactionType.RECHARGE, TransactionStatus.SUCCEEDED, Instant.now());
        transactionRepository.save(tx);

        // When
        var foundTx = transactionRepository.findByStripeTransactionID(stripeTxId);

        // Then
        assertTrue(foundTx.isPresent());
        assertEquals(stripeTxId, foundTx.get().getStripeTransactionID());
    }

    @Test
    void shouldReturnEmptyWhenFindingNonExistentStripeTransactionID() {
        // Given
        var nonExistentStripeTxId = "non_existent";

        // When
        var foundTx = transactionRepository.findByStripeTransactionID(nonExistentStripeTxId);

        // Then
        assertFalse(foundTx.isPresent());
    }
}