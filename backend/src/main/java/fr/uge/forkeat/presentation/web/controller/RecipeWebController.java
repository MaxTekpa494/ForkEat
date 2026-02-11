package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

@Controller
@RequestMapping("/recipes")
public class RecipeWebController {
  private final Logger logger = Logger.getLogger(RecipeWebController.class.getName());
  private final RecipeService recipeService;

  public RecipeWebController(RecipeService recipeService) {
    this.recipeService = Objects.requireNonNull(recipeService);
  }

  @GetMapping
  public String listRecipes(
      @RequestParam(name = "status", defaultValue = "PUBLISHED") String status,
      @RequestParam(name = "size", defaultValue = "12") int size,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "search", required = false) String search,
      @RequestParam(name = "allergens", required = false) List<String> allergens,
      Model model) {
    
    PageResult<Recipe> pageResult;
    if (allergens != null && !allergens.isEmpty()) {
      pageResult = recipeService.findByStatusAndSearchAndAllergens(status, search, allergens, size, page);
    } else if (search != null && !search.isBlank()) {
      pageResult = recipeService.findByStatusAndSearch(status, search, size, page);
    } else {
      pageResult = recipeService.findByStatus(status, size, page);
    }
    
    var recipes = pageResult.items().stream()
        .map(RecipeDTOMapper::toDTO)
        .toList();

    var allAllergens = recipeService.findAllAllergens().stream()
            .map(RecipeDTOMapper::toDTO)
            .toList();

    model.addAttribute("recipes", recipes);
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", (int) Math.ceil((double) pageResult.total() / size));
    model.addAttribute("totalRecipes", pageResult.total());
    model.addAttribute("search", search);
    model.addAttribute("selectedAllergens", allergens);
    model.addAttribute("allAllergens", allAllergens);

    return "recipes/index";
  }

  @GetMapping("/{id}")
  public String viewRecipe(@PathVariable("id") UUID id, Model model) {
    Objects.requireNonNull(id);
    var recipe = recipeService.findById(id);
    var recipeDTO = RecipeDTOMapper.toDTO(recipe);

    if (recipe.isVariant()) {
      var parent = recipeService.findById(recipe.parentId());
      var parentDTO = RecipeDTOMapper.toDTO(parent);
      model.addAttribute("parent", parentDTO);
    }

    model.addAttribute("recipe", recipeDTO);
    return "recipes/detail";
  }
}
