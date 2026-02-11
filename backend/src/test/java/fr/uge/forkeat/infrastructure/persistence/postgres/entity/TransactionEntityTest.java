package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.TransactionType;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import jakarta.persistence.EntityManager;
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
class TransactionEntityTest extends AbstractIntegrationTest {

    private final EntityManager entityManager;

    private WalletEntity wallet;

    @Autowired
    TransactionEntityTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        var user = new UserEntity();
        user.setUsername("tx_user");
        user.setFirstName("Tx");
        user.setLastName("User");
        user.setEmail("tx@test.com");
        user.setPassword("hashed-pw");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        entityManager.persist(user);

        wallet = new WalletEntity(5000L, user);
        entityManager.persist(wallet);
        entityManager.flush();
    }

    @Test
    void shouldCreateTransaction() {
        var tx = new TransactionEntity(null, wallet, 1000L, "stripe_123", TransactionType.RECHARGE, null);

        entityManager.persist(tx);
        entityManager.flush();

        assertNotNull(tx.getId());
        assertNotNull(tx.getCreatedAt());
        assertEquals(1000L, tx.getAmount());
        assertEquals("stripe_123", tx.getStripeTransactionID());
        assertEquals(TransactionType.RECHARGE, tx.getTransactionType());
    }

    @Test
    void shouldThrowOnNegativeAmount() {
        var tx = new TransactionEntity();
        assertThrows(IllegalArgumentException.class, () -> tx.setAmount(-100L));
    }

    @Test
    void shouldSetIdOnPrePersist() {
        var tx = new TransactionEntity(null, wallet, 500L, "stripe_456", TransactionType.RECHARGE, null);

        assertNull(tx.getId());

        entityManager.persist(tx);
        entityManager.flush();

        assertNotNull(tx.getId());
    }

    @Test
    void shouldTestEqualsByIdOnly() {
        var tx1 = new TransactionEntity(null, wallet, 100L, "stripe_eq", TransactionType.RECHARGE, null);
        entityManager.persist(tx1);
        entityManager.flush();

        var tx2 = new TransactionEntity();
        tx2.setId(tx1.getId());

        assertEquals(tx1, tx2);
    }
}
