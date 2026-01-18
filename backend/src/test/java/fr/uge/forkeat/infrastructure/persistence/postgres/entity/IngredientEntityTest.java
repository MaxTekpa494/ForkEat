package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

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
class IngredientEntityTest {

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
    void shouldCreateIngredient() {
        var ingredient = new IngredientEntity("Tomate", "Légume", false);

        entityManager.persist(ingredient);
        entityManager.flush();

        assertNotNull(ingredient.getId());
        assertEquals("Tomate", ingredient.getName());
        assertEquals("Légume", ingredient.getCategory());
        assertFalse(ingredient.getAllergen());
    }

    @Test
    void shouldCreateIngredientAsAllergen() {
        var ingredient = new IngredientEntity("Arachide", "Légumineuse", true);

        entityManager.persist(ingredient);
        entityManager.flush();

        assertNotNull(ingredient.getId());
        assertTrue(ingredient.getAllergen());
    }

    @Test
    void shouldFindIngredientByName() {
        var ingredient = new IngredientEntity("Carotte", "Légume", false);
        entityManager.persist(ingredient);
        entityManager.flush();
        entityManager.clear();

        var found = entityManager
                .createQuery("SELECT i FROM IngredientEntity i WHERE i.name = :name", IngredientEntity.class)
                .setParameter("name", "Carotte")
                .getSingleResult();

        assertNotNull(found);
        assertEquals("Légume", found.getCategory());
    }

    @Test
    void shouldUpdateIngredient() {
        var ingredient = new IngredientEntity("Pomme", "Fruit", false);
        entityManager.persist(ingredient);
        entityManager.flush();

        ingredient.setCategory("Fruit rouge");
        entityManager.flush();
        entityManager.clear();

        var updated = entityManager.find(IngredientEntity.class, ingredient.getId());
        assertEquals("Fruit rouge", updated.getCategory());
    }
}
