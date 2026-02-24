package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.AllergenDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.mapper.ImageMapper;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.*;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

	private record AllergensIngredients(List<AllergenDTO> allergens, List<String> ingredients){}
	private record RecipeWithParent(RecipeDTO recipe, RecipeDTO parent){}

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



	@GetMapping("/create")
	public ResponseEntity<HttpResponse<AllergensIngredients>> pageCreateRecipe() {
		var allAllergens = recipeService.findAllAllergens().stream()
						.map(RecipeDTOMapper::toDTO)
						.toList();
		var allIngredientNames = recipeService.findAllIngredientNames();
		return ResponseEntity.ok(new ItemResponse<>(new AllergensIngredients(allAllergens, allIngredientNames)));
	}

	@PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<HttpResponse<RecipeDTO>> createRecipe(@RequestPart("recipe") RecipeDTO recipeDTO,
																															@RequestPart(value = "image", required = false) MultipartFile image){
		Objects.requireNonNull(recipeDTO);
		var username = authPort.extractUsername();
		var recipe = RecipeDTOMapper.toDomain(RecipeDTOMapper.recipeDTOWithUser(recipeDTO, username));
		var savedRecipe = recipeService.createRecipe(recipe, ImageMapper.toImageUpload(image));
		return ResponseEntity.ok(new CreatedResponse<>(RecipeDTOMapper.toDTO(savedRecipe)));
	}

	@PostAuthorize("returnObject.owner == authentication.name")
	@PostMapping("/{id}/update")
	public ResponseEntity<HttpResponse<RecipeDTO>> updateRecipe(@PathVariable UUID id, @RequestPart RecipeDTO recipeDTO, @RequestPart(value = "image", required = false) MultipartFile image){
		Objects.requireNonNull(recipeDTO);
		var username = authPort.extractUsername();
		var recipeToUpdate = recipeService.findById(id);
		if(!recipeToUpdate.usernameAuthor().equals(username)){
			throw new IllegalStateException("Vous ne pouvez pas modifier une recette qui ne vous appartient pas");
		}
		var recipe = RecipeDTOMapper.toDomain(RecipeDTOMapper.recipeDTOWithUser(recipeDTO, username));
		var updatedRecipe = recipeService.updateRecipe(id, recipe, ImageMapper.toImageUpload(image));
		return ResponseEntity.ok(new ItemResponse<>(RecipeDTOMapper.toDTO(updatedRecipe)));
	}

	//@PostAuthorize("returnObject.owner == authentication.name")
	//@PreAuthorize("returnObject.owner == authentication.name")
	@PostMapping("/{id}/delete")
	public ResponseEntity<HttpResponse<Void>> deleteRecipe(@PathVariable UUID id){
		Objects.requireNonNull(id);
		var username = authPort.extractUsername();
		var recipe = recipeService.findById(id);
		if(!recipe.usernameAuthor().equals(username)){ // AccessDeniedException,
			throw new IllegalStateException("Vous ne pouvez pas supprimer une recette qui ne vous appartient pas");
		}
		recipeService.deleteById(id);
		return ResponseEntity.ok(new NotContentResponse());
	}

	@GetMapping("my-recipes")
	public ResponseEntity<HttpResponse<RecipeDTO>> myRecipes(){
		var username = authPort.extractUsername();
		var recipes = recipeService.findByAuthorUsername(username);
		return ResponseEntity.ok(new ListResponse<>(recipes.stream().map(RecipeDTOMapper::toDTO).toList(), recipes.size()));
	}


	@PostMapping("create-variant")
	public ResponseEntity<HttpResponse<RecipeDTO>> createVariant(@RequestPart RecipeDTO recipeDTO, @RequestPart(value = "image", required = false) MultipartFile image){

		throw new UnsupportedOperationException("Not yet implemented");
	}

}
