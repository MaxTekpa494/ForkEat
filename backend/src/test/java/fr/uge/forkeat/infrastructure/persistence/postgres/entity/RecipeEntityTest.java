package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class RecipeEntityTest extends AbstractIntegrationTest {

    private final EntityManager entityManager;
    private final RecipeRepository recipeRepository;

    private UserEntity author;
    private IngredientEntity ingredientChocolat;
    private IngredientEntity ingredientFarine;
    private AllergenEntity allergenGluten;
    private AllergenEntity allergenLait;

    @Autowired
    public RecipeEntityTest(EntityManager entityManager, RecipeRepository recipeRepository) {
        this.entityManager = entityManager;
        this.recipeRepository = recipeRepository;
    }

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

        ingredientChocolat = new IngredientEntity("Chocolat", "Sucrerie", false);
        ingredientFarine = new IngredientEntity("Farine", "Céréale", false);
        entityManager.persist(ingredientChocolat);
        entityManager.persist(ingredientFarine);

        allergenGluten = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
        allergenLait = new AllergenEntity("Lait", AllergenSeverity.MEDIUM);
        entityManager.persist(allergenGluten);
        entityManager.persist(allergenLait);

        entityManager.flush();
    }

    @Nested
    class CreateRecipe {

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
        void shouldCreateRecipeWithParent() {
            var parentRecipe = new RecipeEntity();
            parentRecipe.setTitle("Recette originale");
            parentRecipe.setSummary("La recette de base");
            parentRecipe.setAuthor(author);
            parentRecipe.setStatus(RecipeStatus.PUBLISHED);
            parentRecipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Étape 1")));
            entityManager.persist(parentRecipe);

            var childRecipe = new RecipeEntity();
            childRecipe.setTitle("Variante de la recette");
            childRecipe.setSummary("Une variante améliorée");
            childRecipe.setAuthor(author);
            childRecipe.setParent(parentRecipe);
            childRecipe.setStatus(RecipeStatus.DRAFT);
            childRecipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Étape modifiée")));
            entityManager.persist(childRecipe);

            entityManager.flush();
            entityManager.clear();

            var found = entityManager.find(RecipeEntity.class, childRecipe.getId());
            assertNotNull(found.getParent());
            assertEquals("Recette originale", found.getParent().getTitle());
        }
    }

    @Nested
    class DeleteRecipe {

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

            var recipeIngredient = new RecipeIngredientEntity(recipe, ingredient, new BigDecimal("100"), "g");
            recipe.addIngredient(recipeIngredient);

            entityManager.persist(recipe);
            entityManager.flush();

            entityManager.remove(recipe);
            entityManager.flush();
            entityManager.clear();

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

            var recipeAllergen = new RecipeAllergenEntity(recipe, allergen);
            recipe.addAllergen(recipeAllergen);

            entityManager.persist(recipe);
            entityManager.flush();

            entityManager.remove(recipe);
            entityManager.flush();
            entityManager.clear();

            var foundAllergen = entityManager.find(AllergenEntity.class, allergenId);
            assertNotNull(foundAllergen);
            assertEquals("Œuf", foundAllergen.getName());
        }
    }

    @Nested
    class UpdateRecipe {

        @Test
        void shouldUpdateRecipeStatus() {
            var recipe = new RecipeEntity();
            recipe.setTitle("Recette en cours");
            recipe.setSummary("En cours de rédaction");
            recipe.setAuthor(author);
            recipe.setStatus(RecipeStatus.DRAFT);
            recipe.setStepByStepInstructions(List.of(new RecipeStep(1, "Étape")));
            entityManager.persist(recipe);
            entityManager.flush();

            recipe.setStatus(RecipeStatus.PENDING_REVIEW);
            entityManager.flush();
            entityManager.clear();

            var found = entityManager.find(RecipeEntity.class, recipe.getId());
            assertEquals(RecipeStatus.PENDING_REVIEW, found.getStatus());
        }
    }

    @Nested
    class SearchRecipes {

        private void createTestRecipesForSearch() {
            var recipe1 = new RecipeEntity();
            recipe1.setTitle("Gâteau au chocolat");
            recipe1.setSummary("Un délicieux gâteau");
            recipe1.setAuthor(author);
            recipe1.setStatus(RecipeStatus.PUBLISHED);
            recipe1.setStepByStepInstructions(List.of(new RecipeStep(1, "Mélanger")));
            recipe1.addIngredient(new RecipeIngredientEntity(recipe1, ingredientChocolat, BigDecimal.TEN, "g"));
            recipe1.addIngredient(new RecipeIngredientEntity(recipe1, ingredientFarine, BigDecimal.TEN, "g"));
            recipe1.addAllergen(new RecipeAllergenEntity(recipe1, allergenGluten));
            entityManager.persist(recipe1);

            var recipe2 = new RecipeEntity();
            recipe2.setTitle("Mousse au chocolat");
            recipe2.setSummary("Une mousse légère");
            recipe2.setAuthor(author);
            recipe2.setStatus(RecipeStatus.PUBLISHED);
            recipe2.setStepByStepInstructions(List.of(new RecipeStep(1, "Battre les oeufs")));
            recipe2.addIngredient(new RecipeIngredientEntity(recipe2, ingredientChocolat, BigDecimal.TEN, "g"));
            entityManager.persist(recipe2);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        void shouldFindRecipesBySearchQuery() {
            long initialCount = recipeRepository.searchRecipes(
                    RecipeStatus.PUBLISHED, "chocolat", List.of(), PageRequest.of(0, 100)).getTotalElements();

            createTestRecipesForSearch();

            var results = recipeRepository.searchRecipes(
                    RecipeStatus.PUBLISHED, "chocolat", List.of(), PageRequest.of(0, 100));

            assertEquals(initialCount + 2, results.getTotalElements());
        }

        @Test
        void shouldExcludeRecipesWithSpecificAllergen() {
            long initialCount = recipeRepository.searchRecipes(
                    RecipeStatus.PUBLISHED, "chocolat", List.of("Gluten"), PageRequest.of(0, 100)).getTotalElements();
            createTestRecipesForSearch();
            var results = recipeRepository.searchRecipes(
                    RecipeStatus.PUBLISHED, "chocolat", List.of("Gluten"), PageRequest.of(0, 100));
            assertEquals(initialCount + 1, results.getTotalElements());
            assertTrue(results.getContent().stream()
                    .anyMatch(r -> r.getTitle().equals("Mousse au chocolat")));
            assertFalse(results.getContent().stream()
                    .anyMatch(r -> r.getTitle().equals("Gâteau au chocolat")));
        }

        @Test
        void shouldNotExcludeRecipesWhenAllergenIsNotPresent() {
            long initialCount = recipeRepository.searchRecipes(
                    RecipeStatus.PUBLISHED, "chocolat", List.of("Lait"), PageRequest.of(0, 100)).getTotalElements();
            createTestRecipesForSearch();
            var results = recipeRepository.searchRecipes(
                    RecipeStatus.PUBLISHED, "chocolat", List.of("Lait"), PageRequest.of(0, 100));
            assertEquals(initialCount + 2, results.getTotalElements());
        }

        @Test
        void shouldReturnEmptyWhenNoMatch() {
            long initialCount = recipeRepository.searchRecipes(
                    RecipeStatus.PUBLISHED, "xyz_introuvable_123", List.of(), PageRequest.of(0, 100)).getTotalElements();
            createTestRecipesForSearch();
            var results = recipeRepository.searchRecipes(
                    RecipeStatus.PUBLISHED, "xyz_introuvable_123", List.of(), PageRequest.of(0, 100));
            assertEquals(initialCount, results.getTotalElements());
        }
    }
}