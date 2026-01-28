package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("forkeat_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&stringtype=unspecified");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

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
