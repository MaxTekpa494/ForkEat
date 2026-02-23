package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("api/recipes")
public final class RecipeRestController {

	private final RecipeService recipeService;
	private final AuthenticationPort authPort;
	private final Logger logger = LoggerFactory.getLogger(RecipeRestController.class);

	public RecipeRestController(RecipeService recipeService, AuthenticationPort authPort) {
		this.recipeService = recipeService;
		this.authPort = authPort;
	}

//	@GetMapping("/create")
//	public ResponseEntity<HttpResponse<Void>> pageCreateRecipe() {
//
//	}

	@PostMapping
	public ResponseEntity<HttpResponse<RecipeDTO>> createRecipe(@RequestBody RecipeDTO recipeDTO) {
		Objects.requireNonNull(recipeDTO);
		var username = authPort.extractUsername();
		var recipe = RecipeDTOMapper.toDomain(RecipeDTOMapper.recipeDTOWithUser(recipeDTO, username));
		var dto = RecipeDTOMapper.toDTO(recipeService.createRecipe(recipe, null));
		return ResponseEntity.ok(new ItemResponse<>(dto));
	}

	// Pourquoi pas faire un findwithparent avec {recipe:..., parent:...}
	// Un endpoint recipe avec juste {recipe:...}
	@GetMapping("/{id}")
	public ResponseEntity<HttpResponse<RecipeDTO>> getRecipe(@PathVariable UUID id) {
		Objects.requireNonNull(id);
		var recipe = recipeService.findById(id);
		var dto = RecipeDTOMapper.toDTO(recipe);
		return ResponseEntity.ok(new ItemResponse<>(dto));
	}

	@GetMapping
	public ResponseEntity<HttpResponse<RecipeDTO>> getRecipes(RecipeSearchDTO form) {
		var criteria = new RecipeSearchCriteria(
				RecipeStatus.valueOf(form.getStatus()), form.getSearch(), form.getAllergens(), form.getSize(), form.getPage());
		var pageResult = recipeService.searchRecipes(criteria);
		logger.debug("Liste ingredients : {}", form.getAllergens());
		var dtos = pageResult.items().stream().map(RecipeDTOMapper::toDTO).toList();
		return ResponseEntity.ok(new ListResponse<>(dtos, pageResult.total()));
	}

}
