package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
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
class IngredientEntityTest extends AbstractIntegrationTest {

    private final EntityManager entityManager;

    @Autowired
    public IngredientEntityTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

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
