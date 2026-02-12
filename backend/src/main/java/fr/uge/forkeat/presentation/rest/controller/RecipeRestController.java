package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("api/recipes")
public final class RecipeRestController {

	private final RecipeService recipeService;
	private final Logger logger = LoggerFactory.getLogger(RecipeRestController.class);

	public RecipeRestController(RecipeService recipeService) {
		this.recipeService = recipeService;
	}

	@PostMapping
	public ResponseEntity<HttpResponse<RecipeDTO>> createRecipe(@RequestBody RecipeDTO recipeDTO) {
		Objects.requireNonNull(recipeDTO);
		var recipe = RecipeDTOMapper.toDomain(recipeDTO);
		var dto = RecipeDTOMapper.toDTO(recipeService.createRecipe(recipe));
		return ResponseEntity.ok(new ItemResponse<>(dto));
	}

	// Pourquoi pas faire un findwithparent avec {recipe:..., parent:...}
	// Un endpoint recipe avec juste {recipe:...}
	@GetMapping("/{id}")
	public ResponseEntity<HttpResponse<RecipeDTO>> getRecipe(@PathVariable("id") UUID id) {
		// findByIdWithParent (parce que c'est une recipe seule), quand c'est une liste
		// de recettes, on les prends les recettes seules sans leurs parents.
		// Finalement je pense qu'on devrait toujours laisser le choix au controller de
		// demander le parent
		// avec un findParent(recipeID) ou findById(parentId)
		Objects.requireNonNull(id);
		var recipe = recipeService.findById(id);
		RecipeDTO recipeParentDTO = null;
		if (recipe.isVariant()) {
			var parent = recipeService.findById(recipe.parentId());
			recipeParentDTO = RecipeDTOMapper.toDTO(parent);
		}
		var dto = RecipeDTOMapper.toDTO(recipe, recipeParentDTO);
		return ResponseEntity.ok(new ItemResponse<>(dto));
	}

	@GetMapping
	public ResponseEntity<HttpResponse<RecipeDTO>> getRecipes(
			@RequestParam(name = "status", defaultValue = "PUBLISHED") String status,
			@RequestParam(name = "size", defaultValue = "10") int size,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "search", required = false) String search,
			@RequestParam(name = "allergens", required = false) List<String> allergens) {
		PageResult<Recipe> pageResult;
		if (allergens != null && !allergens.isEmpty()) {
			pageResult = recipeService.findByStatusAndSearchAndAllergens(status, search, allergens, size, page);
		} else if (search != null && !search.isBlank()) {
			pageResult = recipeService.findByStatusAndSearch(status, search, size, page);
		} else {
			pageResult = recipeService.findByStatus(status, size, page);
		}
		logger.debug("Liste ingredients : {}", allergens);
		var dtos = pageResult.items().stream().map(RecipeDTOMapper::toDTO).toList();
		return ResponseEntity.ok(new ListResponse<>(dtos, pageResult.total()));
	}


}
