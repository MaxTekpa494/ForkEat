package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.web.viewmodel.RecipeListViewModel;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@Controller
@RequestMapping("/recipes")
public class RecipeWebController {
  private final RecipeService recipeService;

  public RecipeWebController(RecipeService recipeService) {
    this.recipeService = recipeService;
  }

  @PostMapping("/create")
  public String pageCreateRecipe() {
    return "recipes/create";
  }

  @PostMapping
  public String createRecipe(RecipeDTO recipeDTO ,Model model) {
    Objects.requireNonNull(recipeDTO);
    var recipe = RecipeDTOMapper.toDomain(recipeDTO);
    var savedRecipe = recipeService.createRecipe(recipe);
    //model.addAttribute("recipe", RecipeDTOMapper.toDTO(savedRecipe));
    return "redirect:/recipes/" + savedRecipe.id();
  }

  @GetMapping
  public String listRecipes(RecipeSearchDTO form, Model model) {
    var criteria = new RecipeSearchCriteria(
            RecipeStatus.valueOf(form.getStatus()), form.getSearch(), form.getAllergens(), form.getSize(), form.getPage());
    var pageResult = recipeService.searchRecipes(criteria);
    var recipes = pageResult.items().stream()
        .map(RecipeDTOMapper::toDTO)
        .toList();
    var allAllergens = recipeService.findAllAllergens().stream()
            .map(RecipeDTOMapper::toDTO)
            .toList();
    var viewModel = new RecipeListViewModel(
            recipes,
            form.getPage(),
            (int) Math.ceil((double) pageResult.total() / form.getSize()),
            pageResult.total(),
            form.getSearch(),
            form.getAllergens(),
            allAllergens
    );
    model.addAttribute("vm", viewModel);
    return "recipes/index";
  }

  @GetMapping("/{id}")
  public String viewRecipe(@PathVariable UUID id, Model model) {
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
