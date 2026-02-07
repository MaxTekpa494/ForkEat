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
class UserEntityTest extends AbstractIntegrationTest {

    private final EntityManager entityManager;

    @Autowired
    public UserEntityTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Test
    void shouldCreateUser() {
        var user = new UserEntity();
        user.setUsername("testuser");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@test.com");
        user.setPassword("hashedpassword123");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);

        entityManager.persist(user);
        entityManager.flush();

        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldCreateUserWithBankInfo() {
        var user = new UserEntity();
        user.setUsername("userWithBank");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane.smith@test.com");
        user.setPassword("hashedpassword456");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);

        var bankInfo = new BankInfoEntity("BNP Paribas", "FR7612345678901234567890123", "BNPAFRPP", user);
        user.setBankInfo(bankInfo);

        entityManager.persist(user);
        entityManager.flush();

        assertNotNull(user.getId());
        assertNotNull(user.getBankInfo());
        assertNotNull(user.getBankInfo().getId());
        assertEquals("FR7612345678901234567890123", user.getBankInfo().getIban());
    }

    @Test
    void shouldCreateUserWithWallet() {
        var user = new UserEntity();
        user.setUsername("userWithWallet");
        user.setFirstName("Bob");
        user.setLastName("Martin");
        user.setEmail("bob.martin@test.com");
        user.setPassword("hashedpassword789");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);

        var wallet = new WalletEntity(1000L, user);
        user.setWallet(wallet);

        entityManager.persist(user);
        entityManager.flush();

        assertNotNull(user.getId());
        assertNotNull(user.getWallet());
        assertEquals(1000L, user.getWallet().getBalance());
    }

    @Test
    void shouldFindUserByEmail() {
        var user = new UserEntity();
        user.setUsername("findableUser");
        user.setFirstName("Alice");
        user.setLastName("Wonder");
        user.setEmail("alice.wonder@test.com");
        user.setPassword("hashedpassword");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.GOOGLE);

        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        var foundUser = entityManager
                .createQuery("SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
                .setParameter("email", "alice.wonder@test.com")
                .getSingleResult();

        assertNotNull(foundUser);
        assertEquals("Alice", foundUser.getFirstName());
        assertEquals(AuthMode.GOOGLE, foundUser.getAuthMode());
    }
}
