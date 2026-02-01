package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecipeEntityTest {

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

    private UserEntity author;

    @BeforeEach
    void setUp() {
        author = new UserEntity();
        author.setUsername("chef");
        author.setFirstName("Arnaud");
        author.setLastName("Carayol");
        author.setEmail("arnaudcarayol@test.com");
        author.setPassword("hashedpassword");
        author.setRole(UserRole.MEMBER);
        author.setStatus(UserStatus.ACTIVE);
        author.setAuthMode(AuthMode.LOCAL);
        entityManager.persist(author);
        entityManager.flush();
    }

    @Test
    void shouldCreateRecipe() {
        var recipe = new RecipeEntity();
        recipe.setTitle("Tarte aux pommes");
        recipe.setSummary("Une délicieuse tarte aux pommes maison");
        recipe.setAuthor(author);
        recipe.setPreparationMinutes(45);
        recipe.setStatus(RecipeStatus.DRAFT);
        recipe.setStepByStepInstructions(List.of(
                new RecipeStep(1, "Préchauffer le four à 180°C"),
                new RecipeStep(2, "Éplucher les pommes")
        ));
        recipe.setDietaryFlag(Map.of("vegetarian", true, "vegan", false, "glutenFree", false));

        entityManager.persist(recipe);
        entityManager.flush();

        assertNotNull(recipe.getId());
        assertNotNull(recipe.getCreatedAt());
        assertNotNull(recipe.getUpdatedAt());
        assertEquals("Tarte aux pommes", recipe.getTitle());
        assertEquals(RecipeStatus.DRAFT, recipe.getStatus());
    }

    @Test
    void shouldCreateRecipeWithIngredients() {
        var ingredient1 = new IngredientEntity("Pomme", "Fruit", false);
        var ingredient2 = new IngredientEntity("Farine", "Céréale", false);
        entityManager.persist(ingredient1);
        entityManager.persist(ingredient2);

        var recipe = new RecipeEntity();
        recipe.setTitle("Tarte aux pommes");
        recipe.setSummary("Une délicieuse tarte");
        recipe.setAuthor(author);
        recipe.setStatus(RecipeStatus.DRAFT);
        recipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Mélanger")));
        recipe.setDietaryFlag(Map.of("vegetarian", true));

        var recipeIngredient1 = new RecipeIngredientEntity(recipe, ingredient1, new BigDecimal("500"), "g");
        var recipeIngredient2 = new RecipeIngredientEntity(recipe, ingredient2, new BigDecimal("250"), "g");
        recipe.addIngredient(recipeIngredient1);
        recipe.addIngredient(recipeIngredient2);

        entityManager.persist(recipe);
        entityManager.flush();
        entityManager.clear();

        var found = entityManager.find(RecipeEntity.class, recipe.getId());
        assertEquals(2, found.getIngredients().size());
    }

    @Test
    void shouldCreateRecipeWithAllergens() {
        var allergen1 = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
        var allergen2 = new AllergenEntity("Lait", AllergenSeverity.MEDIUM);
        entityManager.persist(allergen1);
        entityManager.persist(allergen2);

        var recipe = new RecipeEntity();
        recipe.setTitle("Crêpes");
        recipe.setSummary("Des crêpes bretonnes");
        recipe.setAuthor(author);
        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Mélanger la farine")));
        recipe.setDietaryFlag(Map.of("vegetarian", true));

        var recipeAllergen1 = new RecipeAllergenEntity(recipe, allergen1);
        var recipeAllergen2 = new RecipeAllergenEntity(recipe, allergen2);
        recipe.addAllergen(recipeAllergen1);
        recipe.addAllergen(recipeAllergen2);

        entityManager.persist(recipe);
        entityManager.flush();
        entityManager.clear();

        var found = entityManager.find(RecipeEntity.class, recipe.getId());
        assertEquals(2, found.getAllergens().size());
    }

    @Test
    void shouldNotDeleteIngredientWhenRecipeDeleted() {
        var ingredient = new IngredientEntity("Sucre", "Épicerie", false);
        entityManager.persist(ingredient);
        var ingredientId = ingredient.getId();

        var recipe = new RecipeEntity();
        recipe.setTitle("Gâteau");
        recipe.setSummary("Un gâteau sucré");
        recipe.setAuthor(author);
        recipe.setStatus(RecipeStatus.DRAFT);
        recipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Ajouter le sucre")));
        recipe.setDietaryFlag(Map.of("vegetarian", true));

        var recipeIngredient = new RecipeIngredientEntity(recipe, ingredient, new BigDecimal("100"), "g");
        recipe.addIngredient(recipeIngredient);

        entityManager.persist(recipe);
        entityManager.flush();

        entityManager.remove(recipe);
        entityManager.flush();
        entityManager.clear();

        // L'ingrédient doit toujours exister
        var foundIngredient = entityManager.find(IngredientEntity.class, ingredientId);
        assertNotNull(foundIngredient);
        assertEquals("Sucre", foundIngredient.getName());
    }

    @Test
    void shouldNotDeleteAllergenWhenRecipeDeleted() {
        var allergen = new AllergenEntity("Œuf", AllergenSeverity.HIGH);
        entityManager.persist(allergen);
        var allergenId = allergen.getId();

        var recipe = new RecipeEntity();
        recipe.setTitle("Omelette");
        recipe.setSummary("Une omelette aux œufs");
        recipe.setAuthor(author);
        recipe.setStatus(RecipeStatus.DRAFT);
        recipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Battre les œufs")));
        recipe.setDietaryFlag(Map.of("vegetarian", true));

        var recipeAllergen = new RecipeAllergenEntity(recipe, allergen);
        recipe.addAllergen(recipeAllergen);

        entityManager.persist(recipe);
        entityManager.flush();

        entityManager.remove(recipe);
        entityManager.flush();
        entityManager.clear();

        // L'allergène doit toujours exister
        var foundAllergen = entityManager.find(AllergenEntity.class, allergenId);
        assertNotNull(foundAllergen);
        assertEquals("Œuf", foundAllergen.getName());
    }

    @Test
    void shouldCreateRecipeWithParent() {
        var parentRecipe = new RecipeEntity();
        parentRecipe.setTitle("Recette originale");
        parentRecipe.setSummary("La recette de base");
        parentRecipe.setAuthor(author);
        parentRecipe.setStatus(RecipeStatus.PUBLISHED);
        parentRecipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Étape 1")));
        parentRecipe.setDietaryFlag(Map.of("vegetarian", true));
        entityManager.persist(parentRecipe);

        var childRecipe = new RecipeEntity();
        childRecipe.setTitle("Variante de la recette");
        childRecipe.setSummary("Une variante améliorée");
        childRecipe.setAuthor(author);
        childRecipe.setParent(parentRecipe);
        childRecipe.setStatus(RecipeStatus.DRAFT);
        childRecipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Étape modifiée")));
        childRecipe.setDietaryFlag(Map.of("vegetarian", true, "vegan", true));
        entityManager.persist(childRecipe);

        entityManager.flush();
        entityManager.clear();

        var found = entityManager.find(RecipeEntity.class, childRecipe.getId());
        assertNotNull(found.getParent());
        assertEquals("Recette originale", found.getParent().getTitle());
    }

    @Test
    void shouldUpdateRecipeStatus() {
        var recipe = new RecipeEntity();
        recipe.setTitle("Recette en cours");
        recipe.setSummary("En cours de rédaction");
        recipe.setAuthor(author);
        recipe.setStatus(RecipeStatus.DRAFT);
        recipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Étape")));
        recipe.setDietaryFlag(Map.of("vegetarian", false));
        entityManager.persist(recipe);
        entityManager.flush();

        recipe.setStatus(RecipeStatus.PENDING_REVIEW);
        entityManager.flush();
        entityManager.clear();

        var found = entityManager.find(RecipeEntity.class, recipe.getId());
        assertEquals(RecipeStatus.PENDING_REVIEW, found.getStatus());
    }
}
