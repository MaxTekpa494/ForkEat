package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDetailsDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.UserService;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.user.UserQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("api/recipes")
public final class RecipeRestController {

	private final RecipeService recipeService;
    private final UserService userService;
    private final UserQueryService userQueryService;

	private final Logger logger = LoggerFactory.getLogger(RecipeRestController.class);

	public RecipeRestController(RecipeService recipeService, UserService userService, UserQueryService userQueryService) {
		this.recipeService = recipeService;
        this.userService = userService;
        this.userQueryService = userQueryService;
	}

	@PostMapping
	public ResponseEntity<HttpResponse<RecipeDTO>> createRecipe(@RequestBody RecipeDTO recipeDTO) {
		Objects.requireNonNull(recipeDTO);
		var recipe = RecipeDTOMapper.toDomain(recipeDTO);
		var dto = RecipeDTOMapper.toDTO(recipeService.createRecipe(recipe, null));
		return ResponseEntity.ok(new ItemResponse<>(dto));
	}

	// Pourquoi pas faire un findwithparent avec {recipe:..., parent:...}
	// Un endpoint recipe avec juste {recipe:...}
	@GetMapping("/{id}")
	public ResponseEntity<HttpResponse<RecipeDetailsDTO>> getRecipe(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
		// findByIdWithParent (parce que c'est une recipe seule), quand c'est une liste
		// de recettes, on les prend les recettes seules sans leurs parents.
		// Finalement, je pense qu'on devrait toujours laisser le choix au controller de
		// demander le parent
		// avec un findParent(recipeID) ou findById(parentId)
		Objects.requireNonNull(id);

        User user = null;
        UUID userId = null;
        if(userDetails != null) {
            user = this.userQueryService.getUserByUsername(userDetails.getUsername());
            userId = user.id();
        }
        boolean hasLiked = false;
        if(userId != null){
            hasLiked = userService.hasLikedRecipe(userId, id);
        }

		var recipe = recipeService.findRecipeWithMetaDataById(id);
		RecipeDTO recipeParentDTO = null;
		if (recipe.isVariant()) {
			var parent = recipeService.findById(recipe.parentId());
			recipeParentDTO = RecipeDTOMapper.toDTO(parent);
		}
        //var hasLiked = userService.hasLikedRecipe()
		var dto = RecipeDTOMapper.toRecipeWithMetaDataDTO(recipe, recipeParentDTO, hasLiked);
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

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeRecipe(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.getUserByUsername(userDetails.getUsername());
        this.recipeService.findById(id);
        this.userService.likeRecipe(user.id(), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikeRecipe(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.getUserByUsername(userDetails.getUsername());
        this.recipeService.findById(id);
        this.userService.unlikeRecipe(user.id(), id);
        return ResponseEntity.ok().build();
    }
}
