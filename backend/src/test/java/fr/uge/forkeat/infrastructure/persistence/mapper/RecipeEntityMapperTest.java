package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.AllergenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.IngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeAllergenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeIngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeIngredient;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeEntityMapperTest {

    private UserEntity author;
    private Instant now;

    @BeforeEach
    void setUp() {
        author = new UserEntity();
        author.setId(UUID.randomUUID());
        author.setUsername("chef_arnaud");
        author.setFirstName("Arnaud");
        author.setLastName("Carayol");
        author.setEmail("arnaud@test.com");
        author.setPassword("hashedpassword");
        author.setRole(UserRole.MEMBER);
        author.setStatus(UserStatus.ACTIVE);
        author.setAuthMode(AuthMode.LOCAL);
        now = Instant.now();
    }

    @Test
    void toDomain_shouldConvertEntityToRecipe() {
        var recipeId = UUID.randomUUID();
        var entity = createRecipeEntity(recipeId, "Tarte aux pommes", "Une délicieuse tarte");
        entity.setPreparationMinutes(45);
        entity.setImageUrl("https://example.com/tarte.jpg");
        entity.setStatus(RecipeStatus.PUBLISHED);
        entity.setStepByStepInstructions(List.of(
                new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(1, "Préchauffer le four"),
                new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(2, "Préparer la pâte")
        ));
        entity.setDietaryFlag(Map.of("vegetarian", true, "vegan", false));

        var recipe = RecipeEntityMapper.toDomain(entity);

        assertNotNull(recipe);
        assertEquals(recipeId, recipe.id());
        assertEquals("Tarte aux pommes", recipe.title());
        assertEquals("Une délicieuse tarte", recipe.summary());
        assertEquals("chef_arnaud", recipe.usernameAuthor());
        assertEquals(45, recipe.preparationMinutes());
        assertEquals("https://example.com/tarte.jpg", recipe.imageUrl());
        assertEquals(RecipeStatus.PUBLISHED, recipe.status());
        assertEquals(2, recipe.stepByStepInstructions().size());
        assertTrue(recipe.dietaryFlags().get("vegetarian"));
        assertFalse(recipe.dietaryFlags().get("vegan"));
    }

    @Test
    void toDomain_shouldReturnNullWhenEntityIsNull() {
        var recipe = RecipeEntityMapper.toDomain(null);
        assertNull(recipe);
    }

    @Test
    void toDomain_shouldHandleNullParent() {
        var entity = createRecipeEntity(UUID.randomUUID(), "Test", "Summary");
        entity.setParent(null);

        var recipe = RecipeEntityMapper.toDomain(entity);

        assertNull(recipe.parentId());
        assertFalse(recipe.isVariant());
    }

    @Test
    void toDomain_shouldExtractParentId() {
        var parentId = UUID.randomUUID();
        var parentEntity = createRecipeEntity(parentId, "Parent Recipe", "Original");
        parentEntity.setStatus(RecipeStatus.PUBLISHED);

        var childEntity = createRecipeEntity(UUID.randomUUID(), "Child Recipe", "Variant");
        childEntity.setParent(parentEntity);

        var recipe = RecipeEntityMapper.toDomain(childEntity);

        assertEquals(parentId, recipe.parentId());
        assertTrue(recipe.isVariant());
    }

    @Test
    void toDomain_shouldConvertStepsCorrectly() {
        var entity = createRecipeEntity(UUID.randomUUID(), "Test", "Summary");
        entity.setStepByStepInstructions(List.of(
                new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(1, "Étape 1"),
                new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(2, "Étape 2"),
                new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(3, "Étape 3")
        ));

        var recipe = RecipeEntityMapper.toDomain(entity);

        assertEquals(3, recipe.stepByStepInstructions().size());
        assertEquals(1, recipe.stepByStepInstructions().get(0).stepNumber());
        assertEquals("Étape 1", recipe.stepByStepInstructions().get(0).instruction());
        assertEquals(2, recipe.stepByStepInstructions().get(1).stepNumber());
        assertEquals("Étape 2", recipe.stepByStepInstructions().get(1).instruction());
    }

    @Test
    void toDomain_shouldConvertIngredientsCorrectly() {
        var entity = createRecipeEntity(UUID.randomUUID(), "Test", "Summary");

        var ingredient1 = new IngredientEntity("Farine", "Céréale", false);
        ingredient1.setId(UUID.randomUUID());
        var ingredient2 = new IngredientEntity("Sucre", "Épicerie", false);
        ingredient2.setId(UUID.randomUUID());

        var recipeIngredient1 = new RecipeIngredientEntity(entity, ingredient1, new BigDecimal("250"), "g");
        var recipeIngredient2 = new RecipeIngredientEntity(entity, ingredient2, new BigDecimal("100"), "g");
        entity.addIngredient(recipeIngredient1);
        entity.addIngredient(recipeIngredient2);

        var recipe = RecipeEntityMapper.toDomain(entity);

        assertEquals(2, recipe.ingredients().size());
        assertEquals("Farine", recipe.ingredients().get(0).name());
        assertEquals(250.0, recipe.ingredients().get(0).quantity());
        assertEquals("g", recipe.ingredients().get(0).unit());
    }

    @Test
    void toDomain_shouldConvertAllergensCorrectly() {
        var entity = createRecipeEntity(UUID.randomUUID(), "Test", "Summary");

        var allergen1 = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
        allergen1.setId(UUID.randomUUID());
        var allergen2 = new AllergenEntity("Lait", AllergenSeverity.MEDIUM);
        allergen2.setId(UUID.randomUUID());

        var recipeAllergen1 = new RecipeAllergenEntity(entity, allergen1);
        var recipeAllergen2 = new RecipeAllergenEntity(entity, allergen2);
        entity.addAllergen(recipeAllergen1);
        entity.addAllergen(recipeAllergen2);

        var recipe = RecipeEntityMapper.toDomain(entity);

        assertEquals(2, recipe.allergens().size());
        assertEquals("Gluten", recipe.allergens().get(0).name());
        assertEquals(AllergenSeverity.HIGH, recipe.allergens().get(0).severity());
    }

    @Test
    void toDomain_shouldHandleNullQuantity() {
        var entity = createRecipeEntity(UUID.randomUUID(), "Test", "Summary");

        var ingredient = new IngredientEntity("Sel", "Épicerie", false);
        ingredient.setId(UUID.randomUUID());

        var recipeIngredient = new RecipeIngredientEntity(entity, ingredient, null, "pincée");
        entity.addIngredient(recipeIngredient);

        var recipe = RecipeEntityMapper.toDomain(entity);

        assertEquals(1, recipe.ingredients().size());
        assertEquals(0.0, recipe.ingredients().get(0).quantity());
    }

    @Test
    void toDomain_shouldHandleNullLists() {
        var entity = createRecipeEntity(UUID.randomUUID(), "Test", "Summary");
        // Par défaut, les listes sont initialisées à ArrayList vide dans RecipeEntity

        var recipe = RecipeEntityMapper.toDomain(entity);

        assertNotNull(recipe.stepByStepInstructions());
        assertNotNull(recipe.ingredients());
        assertNotNull(recipe.allergens());
    }

    @Test
    void toEntity_shouldConvertRecipeToEntity() {
        var recipeId = UUID.randomUUID();
        var allergenId = UUID.randomUUID();
        var recipe = new Recipe(
                recipeId,
                "Quiche Lorraine",
                "Une quiche traditionnelle",
                null,
                "chef_arnaud",
                60,
                "https://example.com/quiche.jpg",
                RecipeStatus.PUBLISHED,
                List.of(
                        new fr.uge.forkeat.service.model.recipe.RecipeStep(1, "Préparer la pâte"),
                        new fr.uge.forkeat.service.model.recipe.RecipeStep(2, "Ajouter la garniture")
                ),
                List.of(new RecipeIngredient("Lardons", 200.0, "g")),
                List.of(new Allergen(allergenId, "Lait", AllergenSeverity.MEDIUM)),
                Map.of("vegetarian", false),
                now,
                now
        );

        var allergenEntity = new AllergenEntity("Lait", AllergenSeverity.MEDIUM);
        allergenEntity.setId(allergenId);

        var ingredientEntity = new IngredientEntity("Lardons", "Viande", false);
        ingredientEntity.setId(UUID.randomUUID());

        var entity = RecipeEntityMapper.toEntity(
                recipe, null, author,
                List.of(allergenEntity),
                List.of(ingredientEntity)
        );

        assertNotNull(entity);
        assertEquals(recipeId, entity.getId());
        assertEquals("Quiche Lorraine", entity.getTitle());
        assertEquals("Une quiche traditionnelle", entity.getSummary());
        assertEquals(author, entity.getAuthor());
        assertEquals(60, entity.getPreparationMinutes());
        assertEquals(RecipeStatus.PUBLISHED, entity.getStatus());
        assertEquals(2, entity.getStepByStepInstructions().size());
        assertEquals(1, entity.getIngredients().size());
        assertEquals(1, entity.getAllergens().size());
    }

    @Test
    void toEntity_shouldThrowWhenRecipeIsNull() {
        assertThrows(NullPointerException.class, () ->
                RecipeEntityMapper.toEntity(null, null, author, List.of(), List.of())
        );
    }

    @Test
    void toEntity_shouldThrowWhenAuthorIsNull() {
        var recipe = createMinimalRecipe();
        assertThrows(NullPointerException.class, () ->
                RecipeEntityMapper.toEntity(recipe, null, null, List.of(), List.of())
        );
    }

    @Test
    void toEntity_shouldThrowWhenAllergenEntitiesIsNull() {
        var recipe = createMinimalRecipe();
        assertThrows(NullPointerException.class, () ->
                RecipeEntityMapper.toEntity(recipe, null, author, null, List.of())
        );
    }

    @Test
    void toEntity_shouldThrowWhenIngredientEntitiesIsNull() {
        var recipe = createMinimalRecipe();
        assertThrows(NullPointerException.class, () ->
                RecipeEntityMapper.toEntity(recipe, null, author, List.of(), null)
        );
    }

    @Test
    void toEntity_shouldSetParentCorrectly() {
        var parentId = UUID.randomUUID();
        var parentEntity = createRecipeEntity(parentId, "Parent", "Original");

        var recipe = new Recipe(
                UUID.randomUUID(),
                "Child",
                "Variant",
                parentId,
                "chef_arnaud",
                30,
                null,
                RecipeStatus.DRAFT,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                now,
                now
        );

        var entity = RecipeEntityMapper.toEntity(recipe, parentEntity, author, List.of(), List.of());

        assertNotNull(entity.getParent());
        assertEquals(parentId, entity.getParent().getId());
    }

    @Test
    void toEntity_shouldConvertStepsToEntitySteps() {
        var recipe = new Recipe(
                UUID.randomUUID(),
                "Test",
                "Summary",
                null,
                "chef",
                10,
                null,
                RecipeStatus.DRAFT,
                List.of(
                        new fr.uge.forkeat.service.model.recipe.RecipeStep(1, "Premier"),
                        new fr.uge.forkeat.service.model.recipe.RecipeStep(2, "Deuxième")
                ),
                List.of(),
                List.of(),
                Map.of(),
                now,
                now
        );

        var entity = RecipeEntityMapper.toEntity(recipe, null, author, List.of(), List.of());

        assertEquals(2, entity.getStepByStepInstructions().size());
        assertEquals(1, entity.getStepByStepInstructions().get(0).stepNumber());
        assertEquals("Premier", entity.getStepByStepInstructions().get(0).instruction());
    }

    @Test
    void toEntity_shouldThrowWhenAllergenNotFound() {
        var unknownAllergenId = UUID.randomUUID();
        var recipe = new Recipe(
                UUID.randomUUID(),
                "Test",
                "Summary",
                null,
                "chef",
                10,
                null,
                RecipeStatus.DRAFT,
                List.of(),
                List.of(),
                List.of(new Allergen(unknownAllergenId, "Unknown", AllergenSeverity.LOW)),
                Map.of(),
                now,
                now
        );

        var existingAllergenEntity = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
        existingAllergenEntity.setId(UUID.randomUUID());

        assertThrows(IllegalStateException.class, () ->
                RecipeEntityMapper.toEntity(recipe, null, author, List.of(existingAllergenEntity), List.of())
        );
    }

    @Test
    void toEntity_shouldThrowWhenIngredientNotFound() {
        var recipe = new Recipe(
                UUID.randomUUID(),
                "Test",
                "Summary",
                null,
                "chef",
                10,
                null,
                RecipeStatus.DRAFT,
                List.of(),
                List.of(new RecipeIngredient("Unknown", 100.0, "g")),
                List.of(),
                Map.of(),
                now,
                now
        );

        var existingIngredientEntity = new IngredientEntity("Farine", "Céréale", false);
        existingIngredientEntity.setId(UUID.randomUUID());

        assertThrows(IllegalStateException.class, () ->
                RecipeEntityMapper.toEntity(recipe, null, author, List.of(), List.of(existingIngredientEntity))
        );
    }

    @Test
    void toEntity_shouldMatchIngredientsCaseInsensitive() {
        var recipe = new Recipe(
                UUID.randomUUID(),
                "Test",
                "Summary",
                null,
                "chef",
                10,
                null,
                RecipeStatus.DRAFT,
                List.of(),
                List.of(new RecipeIngredient("FARINE", 250.0, "g")),
                List.of(),
                Map.of(),
                now,
                now
        );

        var ingredientEntity = new IngredientEntity("farine", "Céréale", false);
        ingredientEntity.setId(UUID.randomUUID());

        var entity = RecipeEntityMapper.toEntity(recipe, null, author, List.of(), List.of(ingredientEntity));

        assertEquals(1, entity.getIngredients().size());
        assertEquals("farine", entity.getIngredients().get(0).getIngredient().getName());
    }

    @Test
    void toEntity_shouldConvertQuantityToBigDecimal() {
        var recipe = new Recipe(
                UUID.randomUUID(),
                "Test",
                "Summary",
                null,
                "chef",
                10,
                null,
                RecipeStatus.DRAFT,
                List.of(),
                List.of(new RecipeIngredient("Lait", 0.5, "L")),
                List.of(),
                Map.of(),
                now,
                now
        );

        var ingredientEntity = new IngredientEntity("Lait", "Produit laitier", false);
        ingredientEntity.setId(UUID.randomUUID());

        var entity = RecipeEntityMapper.toEntity(recipe, null, author, List.of(), List.of(ingredientEntity));

        assertEquals(new BigDecimal("0.5"), entity.getIngredients().get(0).getQuantity());
    }

    @Test
    void roundTrip_shouldPreserveBasicData() {
        var recipeId = UUID.randomUUID();
        var originalEntity = createRecipeEntity(recipeId, "Round Trip Test", "Test summary");
        originalEntity.setPreparationMinutes(30);
        originalEntity.setStatus(RecipeStatus.PUBLISHED);
        originalEntity.setStepByStepInstructions(List.of(
                new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(1, "Step 1"),
                new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(2, "Step 2")
        ));
        originalEntity.setDietaryFlag(Map.of("vegetarian", true));

        var recipe = RecipeEntityMapper.toDomain(originalEntity);

        assertEquals(recipeId, recipe.id());
        assertEquals("Round Trip Test", recipe.title());
        assertEquals("Test summary", recipe.summary());
        assertEquals(30, recipe.preparationMinutes());
        assertEquals(RecipeStatus.PUBLISHED, recipe.status());
        assertEquals(2, recipe.stepByStepInstructions().size());
    }

    private RecipeEntity createRecipeEntity(UUID id, String title, String summary) {
        var entity = new RecipeEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setAuthor(author);
        entity.setStatus(RecipeStatus.DRAFT);
        entity.setStepByStepInstructions(List.of(new fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep(1, "Default step")));
        entity.setDietaryFlag(Map.of());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private Recipe createMinimalRecipe() {
        return new Recipe(
                UUID.randomUUID(),
                "Minimal Recipe",
                "Minimal summary",
                null,
                "chef",
                10,
                null,
                RecipeStatus.DRAFT,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                now,
                now
        );
    }
}
