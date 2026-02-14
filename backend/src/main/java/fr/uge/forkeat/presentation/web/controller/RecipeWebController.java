package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.web.viewmodel.RecipeListViewModel;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.UserService;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.user.UserQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@Controller
@RequestMapping("/recipes")
public class RecipeWebController {
  private final RecipeService recipeService;
  private final UserService userService;
  private final UserQueryService  userQueryService;

  public RecipeWebController(RecipeService recipeService, UserService userService, UserQueryService userQueryService) {
    this.recipeService = recipeService;
    this.userService = userService;
    this.userQueryService = userQueryService;
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
  public String viewRecipe(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
    Objects.requireNonNull(id);

    User user = null;
    if(userDetails != null) {
        user = this.userQueryService.getUserByUsername(userDetails.getUsername());
    }

    var recipe = recipeService.findById(id);
    var recipeDTO = RecipeDTOMapper.toDTO(recipe);

    if (recipe.isVariant()) {
      var parent = recipeService.findById(recipe.parentId());
      var parentDTO = RecipeDTOMapper.toDTO(parent);
      model.addAttribute("parent", parentDTO);
    }
    model.addAttribute("nbLike", this.recipeService.nbLike(recipe.id()));
    if(user != null){
        model.addAttribute("hasLiked", this.userService.hasLikedRecipe(user.id(), id));
    }
    model.addAttribute("recipe", recipeDTO);
    return "recipes/detail";
  }

    @PostMapping("/{id}/like")
    public String likeRecipe(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.getUserByUsername(userDetails.getUsername());
        this.userService.likeRecipe(user.id(), id);
        return "redirect:/recipes/" + id;
    }

    @PostMapping("/{id}/unlike")
    public String unlikeRecipe(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.getUserByUsername(userDetails.getUsername());
        this.userService.unlikeRecipe(user.id(), id);
        return "redirect:/recipes/" + id;
    }
}
