package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.mapper.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("api/recipes")
public final class RecipeController {

  private final RecipeService recipeService;

  public RecipeController(RecipeService recipeService){
    this.recipeService = Objects.requireNonNull(recipeService);
  }

  @GetMapping("/{id}")
  public ResponseEntity<HttpResponse<RecipeDTO>> getRecipe( @PathVariable("id") UUID id) {
    // findByIdWithParent (parce que c'est une recipe seule), quand c'est une liste
    // de recettes, on les prends les recettes seules sans leurs parents.
    // Finalement je pense qu'on devrait toujours laisser le choix au controller de demander le parent
    // avec un findParent(recipeID) ou findById(parentId)
    var recipe = recipeService.findById(id);
    RecipeDTO recipeParentDTO = null;
    if(recipe.isVariant()){
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
          @RequestParam(name = "page", defaultValue = "0") int page
  ) {
    var pageResult = recipeService.findByStatus(status, size, page);
    var dtos = pageResult.items().stream()
            .map(RecipeDTOMapper::toDTO)
            .toList();
    return ResponseEntity.ok(new ListResponse<>(dtos, pageResult.total()));
  }
}
