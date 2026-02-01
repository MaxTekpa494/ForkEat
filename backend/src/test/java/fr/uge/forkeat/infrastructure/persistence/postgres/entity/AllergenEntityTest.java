package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AllergenEntityTest {

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
    private EntityManager entityManager;

    @Test
    void shouldCreateAllergen() {
        var allergen = new AllergenEntity("Gluten", AllergenSeverity.HIGH);

        entityManager.persist(allergen);
        entityManager.flush();

        assertNotNull(allergen.getId());
        assertEquals("Gluten", allergen.getName());
        assertEquals(AllergenSeverity.HIGH, allergen.getSeverity());
    }

    @Test
    void shouldCreateAllergenWithCriticalSeverity() {
        var allergen = new AllergenEntity("Arachide", AllergenSeverity.CRITICAL);

        entityManager.persist(allergen);
        entityManager.flush();

        assertNotNull(allergen.getId());
        assertEquals(AllergenSeverity.CRITICAL, allergen.getSeverity());
    }

    @Test
    void shouldFindAllergenByName() {
        var allergen = new AllergenEntity("Lait", AllergenSeverity.MEDIUM);
        entityManager.persist(allergen);
        entityManager.flush();
        entityManager.clear();

        var found = entityManager
                .createQuery("SELECT a FROM AllergenEntity a WHERE a.name = :name", AllergenEntity.class)
                .setParameter("name", "Lait")
                .getSingleResult();

        assertNotNull(found);
        assertEquals(AllergenSeverity.MEDIUM, found.getSeverity());
    }

    @Test
    void shouldUpdateAllergenSeverity() {
        var allergen = new AllergenEntity("Soja", AllergenSeverity.LOW);
        entityManager.persist(allergen);
        entityManager.flush();

        allergen.setSeverity(AllergenSeverity.HIGH);
        entityManager.flush();
        entityManager.clear();

        var updated = entityManager.find(AllergenEntity.class, allergen.getId());
        assertEquals(AllergenSeverity.HIGH, updated.getSeverity());
    }
}
