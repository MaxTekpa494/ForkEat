package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.presentation.dto.recipe.AllergenDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeIngredientDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeStepDTO;
import fr.uge.forkeat.service.model.recipe.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeDTOMapperTest {

    @Test
    void toDTO_shouldConvertRecipeToDTO() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var recipe = new Recipe(
                id,
                "Tarte aux pommes",
                "Une délicieuse tarte",
                null,
                "chef_arnaud",
                45,
                "https://example.com/image.jpg",
                RecipeStatus.PUBLISHED,
                List.of(new RecipeStep(1, "Préchauffer le four")),
                List.of(new RecipeIngredient("Pomme", 500.0, "g")),
                List.of(new Allergen(UUID.randomUUID(), "Gluten", AllergenSeverity.HIGH)),
                Map.of("vegetarian", true, "vegan", false),
                now,
                now
        );

        var dto = RecipeDTOMapper.toDTO(recipe, null);

        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals("Tarte aux pommes", dto.title());
        assertEquals("Une délicieuse tarte", dto.summary());
        assertNull(dto.parent());
        assertEquals("chef_arnaud", dto.username());
        assertEquals(45, dto.preparationMinutes());
        assertEquals("https://example.com/image.jpg", dto.imageUrl());
        assertEquals("PUBLISHED", dto.status());
        assertEquals(1, dto.steps().size());
        assertEquals(1, dto.ingredients().size());
        assertEquals(1, dto.allergens().size());
        assertTrue(dto.dietaryFlags().get("vegetarian"));
        assertFalse(dto.dietaryFlags().get("vegan"));
    }

    @Test
    void toDTO_shouldReturnNullWhenRecipeIsNull() {
        var dto = RecipeDTOMapper.toDTO(null, null);
        assertNull(dto);
    }

    @Test
    void toDTO_shouldIncludeParentDTO() {
        var parentId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        var now = Instant.now();

        var parentDTO = new RecipeDTO(
                parentId, "Recette originale", "La base", null, "chef",
                30, null, "PUBLISHED", List.of(), List.of(), List.of(),
                Map.of(), now, now
        );

        var childRecipe = new Recipe(
                childId, "Variante", "Une variante", parentId, "chef",
                35, null, RecipeStatus.DRAFT, List.of(), List.of(), List.of(),
                Map.of(), now, now
        );

        var dto = RecipeDTOMapper.toDTO(childRecipe, parentDTO);

        assertNotNull(dto.parent());
        assertEquals(parentId, dto.parent().id());
        assertEquals("Recette originale", dto.parent().title());
    }

    @Test
    void toDTO_shouldConvertStepsCorrectly() {
        var recipe = createMinimalRecipe(List.of(
                new RecipeStep(1, "Étape 1"),
                new RecipeStep(2, "Étape 2"),
                new RecipeStep(3, "Étape 3")
        ), List.of(), List.of());

        var dto = RecipeDTOMapper.toDTO(recipe, null);

        assertEquals(3, dto.steps().size());
        assertEquals(1, dto.steps().get(0).stepNumber());
        assertEquals("Étape 1", dto.steps().get(0).instruction());
        assertEquals(2, dto.steps().get(1).stepNumber());
        assertEquals("Étape 2", dto.steps().get(1).instruction());
    }

    @Test
    void toDTO_shouldConvertIngredientsCorrectly() {
        var recipe = createMinimalRecipe(List.of(), List.of(
                new RecipeIngredient("Farine", 250.0, "g"),
                new RecipeIngredient("Lait", 0.5, "L")
        ), List.of());

        var dto = RecipeDTOMapper.toDTO(recipe, null);

        assertEquals(2, dto.ingredients().size());
        assertEquals("Farine", dto.ingredients().getFirst().name());
        assertEquals(250.0, dto.ingredients().getFirst().quantity());
        assertEquals("g", dto.ingredients().getFirst().unit());
    }

    @Test
    void toDTO_shouldConvertAllergensCorrectly() {
        var allergenId = UUID.randomUUID();
        var recipe = createMinimalRecipe(List.of(), List.of(), List.of(
                new Allergen(allergenId, "Gluten", AllergenSeverity.HIGH),
                new Allergen(UUID.randomUUID(), "Lait", AllergenSeverity.MEDIUM)
        ));

        var dto = RecipeDTOMapper.toDTO(recipe, null);

        assertEquals(2, dto.allergens().size());
        assertEquals(allergenId, dto.allergens().getFirst().id());
        assertEquals("Gluten", dto.allergens().getFirst().name());
        assertEquals("HIGH", dto.allergens().getFirst().severity());
    }

    @Test
    void toDTO_shouldHandleNullLists() {
        var id = UUID.randomUUID();
        var recipe = new Recipe(
                id, "Test", "Summary", null, "user",
                10, null, RecipeStatus.DRAFT,
                null, null, null, null,
                null, null
        );

        var dto = RecipeDTOMapper.toDTO(recipe, null);

        assertNotNull(dto.steps());
        assertTrue(dto.steps().isEmpty());
        assertNotNull(dto.ingredients());
        assertTrue(dto.ingredients().isEmpty());
        assertNotNull(dto.allergens());
        assertTrue(dto.allergens().isEmpty());
    }

    @Test
    void toDomain_shouldConvertDTOToRecipe() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var dto = new RecipeDTO(
                id,
                "Crêpes bretonnes",
                "Des crêpes traditionnelles",
                null,
                "chef",
                20,
                "https://example.com/crepes.jpg",
                "PUBLISHED",
                List.of(new RecipeStepDTO(1, "Mélanger la farine")),
                List.of(new RecipeIngredientDTO("Farine", 250.0, "g")),
                List.of(new AllergenDTO(UUID.randomUUID(), "Gluten", "HIGH")),
                Map.of("vegetarian", true),
                now,
                now
        );

        var recipe = RecipeDTOMapper.toDomain(dto, "chef_user");

        assertNotNull(recipe);
        assertEquals(id, recipe.id());
        assertEquals("Crêpes bretonnes", recipe.title());
        assertEquals("Des crêpes traditionnelles", recipe.summary());
        assertEquals("chef_user", recipe.usernameAuthor());
        assertEquals(20, recipe.preparationMinutes());
        assertEquals(RecipeStatus.PUBLISHED, recipe.status());
        assertEquals(1, recipe.stepByStepInstructions().size());
        assertEquals(1, recipe.ingredients().size());
        assertEquals(1, recipe.allergens().size());
    }

    @Test
    void toDomain_shouldThrowWhenDTOIsNull() {
        assertThrows(NullPointerException.class, () ->
                RecipeDTOMapper.toDomain(null, "user")
        );
    }

    @Test
    void toDomain_shouldThrowWhenUsernameIsNull() {
        var dto = new RecipeDTO(
                UUID.randomUUID(), "Test", "Summary", null, "chef",
                10, null, "DRAFT", List.of(), List.of(), List.of(),
                Map.of(), Instant.now(), Instant.now()
        );

        assertThrows(NullPointerException.class, () ->
                RecipeDTOMapper.toDomain(dto, null)
        );
    }

    @Test
    void toDomain_shouldDefaultToDraftStatusWhenNull() {
        var dto = new RecipeDTO(
                UUID.randomUUID(), "Test", "Summary", null, "chef",
                10, null, null, List.of(), List.of(), List.of(),
                Map.of(), Instant.now(), Instant.now()
        );

        var recipe = RecipeDTOMapper.toDomain(dto, "user");

        assertEquals(RecipeStatus.DRAFT, recipe.status());
    }

    @Test
    void toDomain_shouldExtractParentIdFromParentDTO() {
        var parentId = UUID.randomUUID();
        var parentDTO = new RecipeDTO(
                parentId, "Parent", "Parent recipe", null, "chef",
                30, null, "PUBLISHED", List.of(), List.of(), List.of(),
                Map.of(), Instant.now(), Instant.now()
        );

        var dto = new RecipeDTO(
                UUID.randomUUID(), "Child", "Child recipe", parentDTO, "chef",
                25, null, "DRAFT", List.of(), List.of(), List.of(),
                Map.of(), Instant.now(), Instant.now()
        );

        var recipe = RecipeDTOMapper.toDomain(dto, "user");

        assertEquals(parentId, recipe.parentId());
    }

    @Test
    void toDomain_shouldConvertStepsDTOToDomain() {
        var dto = new RecipeDTO(
                UUID.randomUUID(), "Test", "Summary", null, "chef",
                10, null, "DRAFT",
                List.of(
                        new RecipeStepDTO(1, "Premier pas"),
                        new RecipeStepDTO(2, "Deuxième pas")
                ),
                List.of(), List.of(), Map.of(),
                Instant.now(), Instant.now()
        );

        var recipe = RecipeDTOMapper.toDomain(dto, "user");

        assertEquals(2, recipe.stepByStepInstructions().size());
        assertEquals(1, recipe.stepByStepInstructions().getFirst().stepNumber());
        assertEquals("Premier pas", recipe.stepByStepInstructions().getFirst().instruction());
    }

    @Test
    void toDomain_shouldConvertIngredientsDTOToDomain() {
        var dto = new RecipeDTO(
                UUID.randomUUID(), "Test", "Summary", null, "chef",
                10, null, "DRAFT", List.of(),
                List.of(
                        new RecipeIngredientDTO("Sel", 5.0, "g"),
                        new RecipeIngredientDTO("Poivre", 2.0, "g")
                ),
                List.of(), Map.of(),
                Instant.now(), Instant.now()
        );

        var recipe = RecipeDTOMapper.toDomain(dto, "user");

        assertEquals(2, recipe.ingredients().size());
        assertEquals("Sel", recipe.ingredients().getFirst().name());
        assertEquals(5.0, recipe.ingredients().getFirst().quantity());
    }

    @Test
    void toDomain_shouldConvertAllergensDTOToDomain() {
        var allergenId = UUID.randomUUID();
        var dto = new RecipeDTO(
                UUID.randomUUID(), "Test", "Summary", null, "chef",
                10, null, "DRAFT", List.of(), List.of(),
                List.of(new AllergenDTO(allergenId, "Œuf", "CRITICAL")),
                Map.of(),
                Instant.now(), Instant.now()
        );

        var recipe = RecipeDTOMapper.toDomain(dto, "user");

        assertEquals(1, recipe.allergens().size());
        assertEquals(allergenId, recipe.allergens().getFirst().id());
        assertEquals("Œuf", recipe.allergens().getFirst().name());
        assertEquals(AllergenSeverity.CRITICAL, recipe.allergens().getFirst().severity());
    }

    @Test
    void toDomain_shouldHandleNullListsInDTO() {
        var dto = new RecipeDTO(
                UUID.randomUUID(), "Test", "Summary", null, "chef",
                10, null, "DRAFT",
                null, null, null, null,
                Instant.now(), Instant.now()
        );

        var recipe = RecipeDTOMapper.toDomain(dto, "user");

        assertNotNull(recipe.stepByStepInstructions());
        assertTrue(recipe.stepByStepInstructions().isEmpty());
        assertNotNull(recipe.ingredients());
        assertTrue(recipe.ingredients().isEmpty());
    }

    @Test
    void roundTrip_shouldPreserveData() {
        var id = UUID.randomUUID();
        var allergenId = UUID.randomUUID();
        var now = Instant.now();

        var originalRecipe = new Recipe(
                id,
                "Quiche Lorraine",
                "Une quiche traditionnelle",
                null,
                "chef",
                60,
                "https://example.com/quiche.jpg",
                RecipeStatus.PUBLISHED,
                List.of(
                        new RecipeStep(1, "Préparer la pâte"),
                        new RecipeStep(2, "Préparer la garniture")
                ),
                List.of(
                        new RecipeIngredient("Lardons", 200.0, "g"),
                        new RecipeIngredient("Crème fraîche", 30.0, "cl")
                ),
                List.of(new Allergen(allergenId, "Lait", AllergenSeverity.MEDIUM)),
                Map.of("vegetarian", false, "glutenFree", false),
                now,
                now
        );

        var dto = RecipeDTOMapper.toDTO(originalRecipe, null);
        var reconvertedRecipe = RecipeDTOMapper.toDomain(dto, "chef");

        assertEquals(originalRecipe.id(), reconvertedRecipe.id());
        assertEquals(originalRecipe.title(), reconvertedRecipe.title());
        assertEquals(originalRecipe.summary(), reconvertedRecipe.summary());
        assertEquals(originalRecipe.preparationMinutes(), reconvertedRecipe.preparationMinutes());
        assertEquals(originalRecipe.status(), reconvertedRecipe.status());
        assertEquals(originalRecipe.stepByStepInstructions().size(), reconvertedRecipe.stepByStepInstructions().size());
        assertEquals(originalRecipe.ingredients().size(), reconvertedRecipe.ingredients().size());
        assertEquals(originalRecipe.allergens().size(), reconvertedRecipe.allergens().size());
    }

    private Recipe createMinimalRecipe(
            List<RecipeStep> steps,
            List<RecipeIngredient> ingredients,
            List<Allergen> allergens
    ) {
        return new Recipe(
                UUID.randomUUID(),
                "Test Recipe",
                "Test Summary",
                null,
                "test_user",
                10,
                null,
                RecipeStatus.DRAFT,
                steps,
                ingredients,
                allergens,
                Map.of(),
                Instant.now(),
                Instant.now()
        );
    }
}
