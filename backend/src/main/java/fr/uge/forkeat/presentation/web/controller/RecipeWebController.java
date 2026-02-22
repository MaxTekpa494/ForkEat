package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.web.viewmodel.RecipeListViewModel;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/recipes")
public class RecipeWebController {
  private final RecipeService recipeService;
  private final AuthenticationPort authPort;

  private final Logger logger = LoggerFactory.getLogger(RecipeWebController.class);

  public RecipeWebController(RecipeService recipeService, AuthenticationPort authPort) {
    this.recipeService = recipeService;
    this.authPort = authPort;
  }

  @GetMapping("/create")
  public String pageCreateRecipe(Model model) {
    var allAllergens = recipeService.findAllAllergens().stream()
            .map(RecipeDTOMapper::toDTO)
            .toList();
    var allIngredientNames = recipeService.findAllIngredientNames();
    var username = authPort.extractUsername();

    model.addAttribute("allAllergens", allAllergens);
    model.addAttribute("allIngredientNames", allIngredientNames);
    model.addAttribute("selectedAllergenIds", Set.of());
    model.addAttribute("username", username);
    model.addAttribute("formAction", "/recipes");
    model.addAttribute("formTitle", "Créer une recette");
    return "recipes/create";
  }

  @PostMapping
  public String createRecipe(@ModelAttribute RecipeDTO recipeDTO,
                             @RequestPart(value="image", required=false) MultipartFile image,
                             Model model) {
    Objects.requireNonNull(recipeDTO);
    logger.info("Creating recipe 1 {}", recipeDTO);
    var username = authPort.extractUsername();
    var recipe = RecipeDTOMapper.toDomain(RecipeDTOMapper.recipeDTOWithUser(recipeDTO, username));
    logger.info("Creating recipe 2 {}", recipe);
    logger.info("Creating recipe 3 {}", image);
    var savedRecipe = recipeService.createRecipe(recipe, image);
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
