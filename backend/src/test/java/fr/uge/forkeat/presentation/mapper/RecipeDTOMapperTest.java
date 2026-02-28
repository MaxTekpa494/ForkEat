package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.presentation.dto.recipe.AllergenDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeIngredientDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeStepDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeDTOMapperTest {

	@Test
	void toDTO_shouldConvertRecipeToDTO() {
		var id = UUID.randomUUID();
		var now = Instant.now();
		var recipe = new Recipe(id, "Tarte aux pommes", "Une délicieuse tarte", null, "chef_arnaud", 45,
				"https://example.com/image.jpg", RecipeStatus.PUBLISHED,
				List.of(new RecipeStep(1, "Préchauffer le four")), List.of(new RecipeIngredient("Pomme", 500.0, "g")),
				List.of(new Allergen(UUID.randomUUID(), "Gluten", AllergenSeverity.HIGH)),
				List.of("vegetarian"), now, now);

		var dto = RecipeDTOMapper.toDTO(recipe);

		assertNotNull(dto);
		assertEquals(id, dto.id());
		assertEquals("Tarte aux pommes", dto.title());
		assertEquals("Une délicieuse tarte", dto.summary());
		assertNull(dto.parentId());
		assertEquals("chef_arnaud", dto.username());
		assertEquals(45, dto.preparationMinutes());
		assertEquals("https://example.com/image.jpg", dto.imageUrl());
		assertEquals("PUBLISHED", dto.status());
		assertEquals(1, dto.steps().size());
		assertEquals(1, dto.ingredients().size());
		assertEquals(1, dto.allergens().size());
		assertTrue(dto.dietaries().contains("vegetarian"));
		assertFalse(dto.dietaries().contains("vegan"));
	}

	@Test
	void toDTO_shouldIncludeParentDTO() {
		var parentId = UUID.randomUUID();
		var childId = UUID.randomUUID();
		var now = Instant.now();

		var parentDTO = new RecipeDTO(parentId, "Recette originale", "La base", null, "chef", 30, null, "PUBLISHED",
				List.of(), List.of(), List.of(), List.of(), now, now);

		var childRecipe = new Recipe(childId, "Variante", "Une variante", parentId, "chef", 35, null,
				RecipeStatus.DRAFT, List.of(), List.of(), List.of(), List.of(), now, now);

		var dto = RecipeDTOMapper.toDTO(childRecipe);

		assertNotNull(dto.parentId());
		assertEquals(parentId, dto.parentId());
		assertEquals("Variante", dto.title());
		//		assertEquals("Recette originale", dto.parent().title()); // Ce test ne sert plus à rien car maintenant on juste id
	}

	@Test
	void toDTO_shouldConvertStepsCorrectly() {
		var recipe = createMinimalRecipe(
				List.of(new RecipeStep(1, "Étape 1"), new RecipeStep(2, "Étape 2"), new RecipeStep(3, "Étape 3")),
				List.of(), List.of());

		var dto = RecipeDTOMapper.toDTO(recipe);

		assertEquals(3, dto.steps().size());
		assertEquals(1, dto.steps().get(0).stepNumber());
		assertEquals("Étape 1", dto.steps().get(0).instruction());
		assertEquals(2, dto.steps().get(1).stepNumber());
		assertEquals("Étape 2", dto.steps().get(1).instruction());
	}

	@Test
	void toDTO_shouldConvertIngredientsCorrectly() {
		var recipe = createMinimalRecipe(List.of(),
				List.of(new RecipeIngredient("Farine", 250.0, "g"), new RecipeIngredient("Lait", 0.5, "L")), List.of());

		var dto = RecipeDTOMapper.toDTO(recipe);

		assertEquals(2, dto.ingredients().size());
		assertEquals("Farine", dto.ingredients().getFirst().name());
		assertEquals(250.0, dto.ingredients().getFirst().quantity());
		assertEquals("g", dto.ingredients().getFirst().unit());
	}

	@Test
	void toDTO_shouldConvertAllergensCorrectly() {
		var allergenId = UUID.randomUUID();
		var recipe = createMinimalRecipe(List.of(), List.of(),
				List.of(new Allergen(allergenId, "Gluten", AllergenSeverity.HIGH),
						new Allergen(UUID.randomUUID(), "Lait", AllergenSeverity.MEDIUM)));

		var dto = RecipeDTOMapper.toDTO(recipe);

		assertEquals(2, dto.allergens().size());
		assertEquals(allergenId, dto.allergens().getFirst().id());
		assertEquals("Gluten", dto.allergens().getFirst().name());
		assertEquals("HIGH", dto.allergens().getFirst().severity());
	}

	@Test
	void toDTO_shouldHandleNullLists() {
		var id = UUID.randomUUID();
		var recipe = new Recipe(id, "Test", "Summary", null, "user", 10, null, RecipeStatus.DRAFT, null, null, null,
				null, null, null);

		var dto = RecipeDTOMapper.toDTO(recipe);

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
		var dto = new RecipeDTO(id, "Crêpes bretonnes", "Des crêpes traditionnelles", null, "chef_user", 20,
				"https://example.com/crepes.jpg", "PUBLISHED", List.of(new RecipeStepDTO(1, "Mélanger la farine")),
				List.of(new RecipeIngredientDTO("Farine", 250.0, "g")),
				List.of(new AllergenDTO(UUID.randomUUID(), "Gluten", "HIGH")), List.of("vegetarian"), now, now);

		var recipe = RecipeDTOMapper.toDomain(dto);

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
		assertThrows(NullPointerException.class, () -> RecipeDTOMapper.toDomain(null));
	}

	@Test
	void toDomain_shouldThrowWhenUsernameIsNull() {
		var dto = new RecipeDTO(UUID.randomUUID(), "Test", "Summary", null, null, 10, null, "DRAFT", List.of(),
				List.of(), List.of(), List.of(), Instant.now(), Instant.now());

		assertThrows(NullPointerException.class, () -> RecipeDTOMapper.toDomain(dto));
	}

	@Test
	void toDomain_shouldDefaultToDraftStatusWhenNull() {
		var dto = new RecipeDTO(UUID.randomUUID(), "Test", "Summary", null, "chef", 10, null, null, List.of(),
				List.of(), List.of(), List.of(), Instant.now(), Instant.now());

		var recipe = RecipeDTOMapper.toDomain(dto);

		assertEquals(RecipeStatus.DRAFT, recipe.status());
	}

	@Test
	void toDomain_shouldExtractParentIdFromParentDTO() {
		var parentId = UUID.randomUUID();
		var parentDTO = new RecipeDTO(parentId, "Parent", "Parent recipe", null, "chef", 30, null, "PUBLISHED",
				List.of(), List.of(), List.of(), List.of(), Instant.now(), Instant.now());

		var dto = new RecipeDTO(UUID.randomUUID(), "Child", "Child recipe", parentDTO.id(), "chef", 25, null, "DRAFT",
				List.of(), List.of(), List.of(), List.of(), Instant.now(), Instant.now());

		var recipe = RecipeDTOMapper.toDomain(dto);

		assertEquals(parentId, recipe.parentId());
	}

	@Test
	void toDomain_shouldConvertStepsDTOToDomain() {
		var dto = new RecipeDTO(UUID.randomUUID(), "Test", "Summary", null, "chef", 10, null, "DRAFT",
				List.of(new RecipeStepDTO(1, "Premier pas"), new RecipeStepDTO(2, "Deuxième pas")), List.of(),
				List.of(), List.of(), Instant.now(), Instant.now());

		var recipe = RecipeDTOMapper.toDomain(dto);

		assertEquals(2, recipe.stepByStepInstructions().size());
		assertEquals(1, recipe.stepByStepInstructions().getFirst().stepNumber());
		assertEquals("Premier pas", recipe.stepByStepInstructions().getFirst().instruction());
	}

	@Test
	void toDomain_shouldConvertIngredientsDTOToDomain() {
		var dto = new RecipeDTO(UUID.randomUUID(), "Test", "Summary", null, "chef", 10, null, "DRAFT", List.of(),
				List.of(new RecipeIngredientDTO("Sel", 5.0, "g"), new RecipeIngredientDTO("Poivre", 2.0, "g")),
				List.of(), List.of(), Instant.now(), Instant.now());

		var recipe = RecipeDTOMapper.toDomain(dto);

		assertEquals(2, recipe.ingredients().size());
		assertEquals("Sel", recipe.ingredients().getFirst().name());
		assertEquals(5.0, recipe.ingredients().getFirst().quantity());
	}

	@Test
	void toDomain_shouldConvertAllergensDTOToDomain() {
		var allergenId = UUID.randomUUID();
		var dto = new RecipeDTO(UUID.randomUUID(), "Test", "Summary", null, "chef", 10, null, "DRAFT", List.of(),
				List.of(), List.of(new AllergenDTO(allergenId, "Œuf", "CRITICAL")), List.of(), Instant.now(),
				Instant.now());

		var recipe = RecipeDTOMapper.toDomain(dto);

		assertEquals(1, recipe.allergens().size());
		assertEquals(allergenId, recipe.allergens().getFirst().id());
		assertEquals("Œuf", recipe.allergens().getFirst().name());
		assertEquals(AllergenSeverity.CRITICAL, recipe.allergens().getFirst().severity());
	}

	@Test
	void toDomain_shouldHandleNullListsInDTO() {
		var dto = new RecipeDTO(UUID.randomUUID(), "Test", "Summary", null, "chef", 10, null, "DRAFT", null, null, null,
				null, Instant.now(), Instant.now());

		var recipe = RecipeDTOMapper.toDomain(dto);

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

		var originalRecipe = new Recipe(id, "Quiche Lorraine", "Une quiche traditionnelle", null, "chef", 60,
				"https://example.com/quiche.jpg", RecipeStatus.PUBLISHED,
				List.of(new RecipeStep(1, "Préparer la pâte"), new RecipeStep(2, "Préparer la garniture")),
				List.of(new RecipeIngredient("Lardons", 200.0, "g"), new RecipeIngredient("Crème fraîche", 30.0, "cl")),
				List.of(new Allergen(allergenId, "Lait", AllergenSeverity.MEDIUM)),
				List.of(), now, now);

		var dto = RecipeDTOMapper.toDTO(originalRecipe);
		var reconvertedRecipe = RecipeDTOMapper.toDomain(dto);

		assertEquals(originalRecipe.id(), reconvertedRecipe.id());
		assertEquals(originalRecipe.title(), reconvertedRecipe.title());
		assertEquals(originalRecipe.summary(), reconvertedRecipe.summary());
		assertEquals(originalRecipe.preparationMinutes(), reconvertedRecipe.preparationMinutes());
		assertEquals(originalRecipe.status(), reconvertedRecipe.status());
		assertEquals(originalRecipe.stepByStepInstructions().size(), reconvertedRecipe.stepByStepInstructions().size());
		assertEquals(originalRecipe.ingredients().size(), reconvertedRecipe.ingredients().size());
		assertEquals(originalRecipe.allergens().size(), reconvertedRecipe.allergens().size());
	}

	@Test
	void toSummaryDTO_shouldMapAllFields() {
		var id = UUID.randomUUID();
		var now = Instant.now();
		var summary = new RecipeSummary(id, "Tarte aux pommes", "Une délicieuse tarte", "https://img.com/tarte.jpg", 45, now, "chef_test");
		var counts = new RecipeCounts(10L, 3L);
		var interaction = new RecipeUserInteraction(true, false);
		var personalized = new PersonalizedRecipeSummary(summary, counts, interaction);

		var dto = RecipeDTOMapper.toSummaryDTO(personalized);

		assertNotNull(dto);
		assertEquals(id, dto.id());
		assertEquals("Tarte aux pommes", dto.title());
		assertEquals("Une délicieuse tarte", dto.summary());
		assertEquals("https://img.com/tarte.jpg", dto.imageUrl());
		assertEquals(45, dto.preparationMinutes());
		assertEquals("chef_test", dto.authorUsername());
		assertEquals(10L, dto.likeCount());
		assertEquals(3L, dto.superLikeCount());
		assertTrue(dto.likedByCurrentUser());
		assertFalse(dto.superLikedByCurrentUser());
	}

	@Test
	void toSummaryDTO_shouldThrowWhenNull() {
		assertThrows(NullPointerException.class, () -> RecipeDTOMapper.toSummaryDTO(null));
	}

	private Recipe createMinimalRecipe(List<RecipeStep> steps, List<RecipeIngredient> ingredients,
			List<Allergen> allergens) {
		return new Recipe(UUID.randomUUID(), "Test Recipe", "Test Summary", null, "test_user", 10, null,
				RecipeStatus.DRAFT, steps, ingredients, allergens, List.of(), Instant.now(), Instant.now());
	}
}
