package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import jakarta.persistence.EntityManager;
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
class WalletEntityTest extends AbstractIntegrationTest {

    private final EntityManager entityManager;

    @Autowired
    WalletEntityTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private UserEntity createAndPersistUser(String username, String email) {
        var user = new UserEntity();
        user.setUsername(username);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("hashed-pw");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    @Test
    void shouldCreateWalletWithUser() {
        var user = createAndPersistUser("wallet_user", "wallet@test.com");
        var wallet = new WalletEntity(500L, user);

        entityManager.persist(wallet);
        entityManager.flush();

        assertNotNull(wallet.getId());
        assertEquals(500L, wallet.getBalance());
        assertNotNull(wallet.getUpdatedAt());
        assertEquals(user.getId(), wallet.getUser().getId());
    }

    @Test
    void shouldDefaultBalanceToZero() {
        var user = createAndPersistUser("zero_wallet", "zero@test.com");
        var wallet = new WalletEntity(0L, user);

        entityManager.persist(wallet);
        entityManager.flush();

        assertEquals(0L, wallet.getBalance());
    }

    @Test
    void shouldUpdateBalance() {
        var user = createAndPersistUser("update_wallet", "update@test.com");
        var wallet = new WalletEntity(1000L, user);

        entityManager.persist(wallet);
        entityManager.flush();

        wallet.setBalance(2000L);
        entityManager.flush();

        entityManager.clear();
        var found = entityManager.find(WalletEntity.class, wallet.getId());
        assertEquals(2000L, found.getBalance());
    }

    @Test
    void shouldSetIdOnPrePersist() {
        var user = createAndPersistUser("prepersist_user", "prepersist@test.com");
        var wallet = new WalletEntity(100L, user);

        assertNull(wallet.getId());

        entityManager.persist(wallet);
        entityManager.flush();

        assertNotNull(wallet.getId());
        assertNotNull(wallet.getUpdatedAt());
    }

    @Test
    void shouldTestEqualsByIdOnly() {
        var user = createAndPersistUser("eq_user", "eq@test.com");
        var wallet1 = new WalletEntity(100L, user);
        entityManager.persist(wallet1);
        entityManager.flush();

        var wallet2 = new WalletEntity();
        wallet2.setId(wallet1.getId());

        assertEquals(wallet1, wallet2);
    }
}
