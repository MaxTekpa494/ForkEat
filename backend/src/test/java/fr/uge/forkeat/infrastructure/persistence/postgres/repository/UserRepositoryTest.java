package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class UserRepositoryTest extends AbstractIntegrationTest {

    private final UserRepository userRepository;

    @Autowired
    public UserRepositoryTest(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Test
    void shouldSaveAndFindUserByUsername() {
        var user = new UserEntity();
        user.setUsername("padela");
        user.setEmail("padela@example.com");
        user.setFirstName("panini");
        user.setLastName("piani");
        user.setPassword("test");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);

        userRepository.save(user);

        var found = userRepository.findByUsername("padela");
        assertTrue(found.isPresent());
        assertEquals("panini", found.get().getFirstName());
    }

    @Test
    void shouldCheckIfUsernameExists() {
        var user = new UserEntity();
        user.setUsername("existsUser");
        user.setEmail("exists@example.com");
        user.setFirstName("padel");
        user.setLastName("pennour");
        user.setPassword("test");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);

        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("existsUser"));
        assertFalse(userRepository.existsByUsername("notExists"));
    }

    @Test
    void shouldFindUserByEmail() {
        UserEntity user = new UserEntity();
        user.setUsername("emailUser");
        user.setEmail("email@test.com");
        user.setFirstName("Pierno");
        user.setLastName("py");
        user.setPassword("hashed");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);

        userRepository.save(user);

        var found = userRepository.findByEmail("email@test.com");
        assertTrue(found.isPresent());
        assertEquals("Pierno", found.get().getFirstName());
    }
}
