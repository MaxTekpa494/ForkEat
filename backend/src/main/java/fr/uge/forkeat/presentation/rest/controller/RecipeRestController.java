package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.*;
import fr.uge.forkeat.presentation.mapper.ImageMapper;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.*;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
  private final UserService userService;
  private final Logger logger = LoggerFactory.getLogger(RecipeRestController.class);

  public RecipeRestController(RecipeService recipeService, AuthenticationPort authPort, UserService userService) {
    this.recipeService = recipeService;
    this.authPort = authPort;
    this.userService = userService;
  }

  public record AllergensIngredients(List<AllergenDTO> allergens, List<String> ingredients, List<String> dietaries) {
  }

  public record RecipeData(RecipeDetailsDTO recipe, RecipeDTO parent, RecipeDiff diff) {
  }

  // Pourquoi pas faire un findwithparent avec {recipe:..., parent:...}
  // Un endpoint recipe avec juste {recipe:...}
  @GetMapping("/{id}")
  public ResponseEntity<HttpResponse<RecipeData>> getRecipe(@PathVariable UUID id) {
    Objects.requireNonNull(id);
    String currentUsername = null;
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      var extracted = authPort.extractUsername();
      if (extracted != null && !extracted.equals("anonymousUser")) {
        currentUsername = extracted;
      }
    }
    var personalizedRecipe = recipeService.findPersonalizedRecipeById(id, currentUsername);
    var recipe = personalizedRecipe.recipe();
    if (!recipe.isPublished() && (currentUsername == null || !currentUsername.equals(recipe.usernameAuthor()))) {
      throw new RecipeNotFoundException(id);
    }
    RecipeDTO parentDTO = null;
    RecipeDiff diff = null;
    if (recipe.parentId() != null) {
      var recipeParent = recipeService.findById(personalizedRecipe.recipe().parentId());
      parentDTO = RecipeDTOMapper.toDTO(recipeParent);
      diff = RecipeDiff.compute(parentDTO, RecipeDTOMapper.toDTO(personalizedRecipe.recipe()));
    }
    var dto = RecipeDTOMapper.toPersonalizedRecipeDTO(personalizedRecipe);
    return ResponseEntity.ok(new ItemResponse<>(new RecipeData(dto, parentDTO, diff)));
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
    var allDietaryNames = recipeService.findAllDietaryNames();
    return ResponseEntity.ok(new ItemResponse<>(new AllergensIngredients(allAllergens, allIngredientNames, allDietaryNames)));
  }

  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<HttpResponse<RecipeDTO>> createRecipe(@RequestPart("recipe") RecipeDTO recipeDTO,
                                                              @RequestPart(value = "image", required = false) MultipartFile image) {
    Objects.requireNonNull(recipeDTO);
    var username = authPort.extractUsername();
    var recipe = RecipeDTOMapper.toDomain(RecipeDTOMapper.recipeDTOWithUser(recipeDTO, username));
    var savedRecipe = recipeService.createRecipe(recipe, ImageMapper.toImageUpload(image));
    return ResponseEntity.ok(new CreatedResponse<>(RecipeDTOMapper.toDTO(savedRecipe)));
  }

  @PostMapping(value = "/{id}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<HttpResponse<RecipeDTO>> updateRecipe(@PathVariable UUID id, @RequestPart("recipe") RecipeDTO recipeDTO, @RequestPart(value = "image", required = false) MultipartFile image) {
    Objects.requireNonNull(recipeDTO);
    var username = authPort.extractUsername();
    var recipeToUpdate = recipeService.findById(id);
    if (!recipeToUpdate.usernameAuthor().equals(username)) {
      throw new IllegalStateException("Vous ne pouvez pas modifier une recette qui ne vous appartient pas");
    }
    var recipe = RecipeDTOMapper.toDomain(RecipeDTOMapper.recipeDTOWithUser(recipeDTO, username));
    var updatedRecipe = recipeService.updateRecipe(id, recipe, ImageMapper.toImageUpload(image));
    return ResponseEntity.ok(new ItemResponse<>(RecipeDTOMapper.toDTO(updatedRecipe)));
  }

  @PostMapping("/{id}/delete")
  public ResponseEntity<HttpResponse<Void>> deleteRecipe(@PathVariable UUID id) {
    Objects.requireNonNull(id);
    var username = authPort.extractUsername();
    var recipe = recipeService.findById(id);
    if (!recipe.usernameAuthor().equals(username)) {
      throw new IllegalStateException("Vous ne pouvez pas supprimer une recette qui ne vous appartient pas");
    }
    recipeService.deleteById(id);
    return ResponseEntity.ok(new NotContentResponse());
  }

  @GetMapping("my-recipes")
  public ResponseEntity<HttpResponse<RecipeDTO>> myRecipes() {
    var username = authPort.extractUsername();
    var recipes = recipeService.findByAuthorUsername(username);
    return ResponseEntity.ok(new ListResponse<>(recipes.stream().map(RecipeDTOMapper::toDTO).toList(), recipes.size()));
  }

  @PostMapping(value = "create-variant", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<HttpResponse<RecipeDTO>> createVariant(@RequestPart("recipe") RecipeDTO recipeDTO, @RequestPart(value = "image", required = false) MultipartFile image) {
		Objects.requireNonNull(recipeDTO);
		var currentUser = authPort.extractUsername();
		var dto = RecipeDTOMapper.recipeDTOWithUser(recipeDTO, currentUser);
		var hasNewImage = image != null && !image.isEmpty();
		if (!hasNewImage && recipeDTO.parentId() != null) {
			var parent = recipeService.findById(recipeDTO.parentId());
			logger.info("Adding image from parent {}\n\n\n", parent);
			dto = RecipeDTOMapper.recipeDTOWithImageUrl(dto, parent.imageUrl());
		}
		var savedRecipe = recipeService.createRecipe(RecipeDTOMapper.toDomain(dto), hasNewImage ? ImageMapper.toImageUpload(image) : null);
		return ResponseEntity.ok(new CreatedResponse<>(RecipeDTOMapper.toDTO(savedRecipe)));
  }

  @PostMapping("/{id}/like")
  public ResponseEntity<?> likeRecipe(@PathVariable UUID id) {
    var user = userService.getUserByUsername(authPort.extractUsername());
    recipeService.likeRecipe(user.id(), id);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}/like")
  public ResponseEntity<?> unlikeRecipe(@PathVariable UUID id) {
    var user = userService.getUserByUsername(authPort.extractUsername());
    recipeService.unlikeRecipe(user.id(), id);
    return ResponseEntity.ok().build();
  }
}
