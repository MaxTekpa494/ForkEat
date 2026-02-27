package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.recipe.AllergenDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDiff;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.mapper.ImageMapper;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.web.viewmodel.RecipeListViewModel;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.user.UserService;
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
    private final UserService userService;

    private final Logger logger = LoggerFactory.getLogger(RecipeWebController.class);

    public RecipeWebController(RecipeService recipeService, UserService userService, AuthenticationPort authPort) {
        this.recipeService = recipeService;
        this.userService = userService;
        this.authPort = authPort;
    }

    @GetMapping("/create")
    public String pageCreateRecipe(Model model) {
        var allAllergens = recipeService.findAllAllergens().stream()
                .map(RecipeDTOMapper::toDTO)
                .toList();
        var allIngredientNames = recipeService.findAllIngredientNames();
        var allUnitNames = recipeService.findAllUnitNames();
        var allDietaryNames = recipeService.findAllDietaryNames();
        var username = authPort.extractUsername();

        model.addAttribute("allAllergens", allAllergens);
        model.addAttribute("allIngredientNames", allIngredientNames);
        model.addAttribute("allUnitNames", allUnitNames);
        model.addAttribute("allDietaryNames", allDietaryNames);
        model.addAttribute("selectedAllergenIds", Set.of());
        model.addAttribute("username", username);
        model.addAttribute("formAction", "/recipes");
        model.addAttribute("formTitle", "Créer une recette");
        return "recipes/create";
    }

    @PostMapping
    public String createRecipe(@ModelAttribute RecipeDTO recipeDTO,
                               @RequestPart(value = "image", required = false) MultipartFile image,
                               Model model) {
        Objects.requireNonNull(recipeDTO);
        logger.info("Creating recipe 1 {}", recipeDTO);
        var username = authPort.extractUsername();
        var recipe = RecipeDTOMapper.toDomain(RecipeDTOMapper.recipeDTOWithUser(recipeDTO, username));
        logger.info("Creating recipe 2 {}", recipe);
        logger.info("Creating recipe status 2 {}", recipe.status());
        logger.info("Creating recipe 3 {}", image);
        var savedRecipe = recipeService.createRecipe(recipe, ImageMapper.toImageUpload(image));
        return "redirect:/recipes/" + savedRecipe.id();
    }


    @GetMapping("/my-recipes")
    public String myRecipes(Model model) {
        var username = authPort.extractUsername();
        var myRecipes = recipeService.findByAuthorUsername(username);
        var recipesDTO = myRecipes.stream()
                .map(RecipeDTOMapper::toDTO)
                .toList();

        model.addAttribute("recipes", recipesDTO);
        model.addAttribute("username", username);
        return "recipes/my-recipes";
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
        var currentUser = authPort.extractUsername();
        var personalizedRecipe = recipeService.findPersonalizedRecipeById(id, currentUser);
        var recipe = personalizedRecipe.recipe();

        if (!recipe.isPublished() && !authPort.isAdmin() && (currentUser == null || !currentUser.equals(recipe.usernameAuthor()))) {
            throw new RecipeNotFoundException(id);
        }

        var recipeDTO = RecipeDTOMapper.toPersonalizedRecipeDTO(personalizedRecipe);

        if (recipe.isVariant()) {
            var parent = recipeService.findById(recipe.parentId());
            var parentDTO = RecipeDTOMapper.toDTO(parent);
            model.addAttribute("parent", parentDTO);
            model.addAttribute("diff", RecipeDiff.compute(parentDTO, recipeDTO.toRecipeDTO()));
        }

        var isOwner = currentUser != null && currentUser.equals(recipe.usernameAuthor());
        var hasActiveDietaryFlags = recipeDTO.dietaries() != null && !recipeDTO.dietaries().isEmpty();
        model.addAttribute("recipe", recipeDTO);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAuthenticated", currentUser != null);
        model.addAttribute("hasActiveDietaryFlags", hasActiveDietaryFlags);
        logger.info("Recipe {} viewed by {}", recipe, currentUser);
        return "recipes/detail";
    }

    @PostMapping("/{id}/delete")
    public String deleteRecipe(@PathVariable UUID id) {
        var currentUser = authPort.extractUsername();
        var recipe = recipeService.findById(id);

        if (!currentUser.equals(recipe.usernameAuthor())) {
            throw new IllegalStateException("Vous ne pouvez pas supprimer une recette qui ne vous appartient pas");
        }

        recipeService.deleteById(id);
        return "redirect:/recipes/my-recipes";
    }

    @GetMapping("/{id}/edit")
    public String editRecipe(@PathVariable UUID id, Model model) {
        var currentUser = authPort.extractUsername();
        var recipe = recipeService.findById(id);
        logger.info("Editing recipe {}", recipe);
        if (!currentUser.equals(recipe.usernameAuthor())) {
            model.addAttribute("errorMessage", "Vous ne pouvez pas modifier une recette qui ne vous appartient pas");
            model.addAttribute("pageTitle", "Accès non autorisé");
            return "error/404";
        }

        var recipeDTO = RecipeDTOMapper.toDTO(recipe);
        var allAllergens = recipeService.findAllAllergens().stream()
                .map(RecipeDTOMapper::toDTO)
                .toList();
        var allIngredientNames = recipeService.findAllIngredientNames();
        var allUnitNames = recipeService.findAllUnitNames();
        var allDietaryNames = recipeService.findAllDietaryNames();
        var selectedAllergenIds = recipeDTO.allergens().stream().map(AllergenDTO::id).toList();

        model.addAttribute("recipe", recipeDTO);
        model.addAttribute("allAllergens", allAllergens);
        model.addAttribute("allIngredientNames", allIngredientNames);
        model.addAttribute("allUnitNames", allUnitNames);
        model.addAttribute("allDietaryNames", allDietaryNames);
        model.addAttribute("selectedAllergenIds", selectedAllergenIds);
        model.addAttribute("username", currentUser);
        model.addAttribute("formAction", "/recipes/" + id + "/edit");
        model.addAttribute("formTitle", "Modifier la recette");

        return "recipes/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateRecipe(@PathVariable UUID id,
                               @ModelAttribute RecipeDTO recipeDTO,
                               @RequestPart(value = "image", required = false) MultipartFile image,
                               Model model) {
        var currentUser = authPort.extractUsername();
        var existingRecipe = recipeService.findById(id);

        if (!currentUser.equals(existingRecipe.usernameAuthor())) {
            throw new IllegalStateException("Vous ne pouvez pas modifier une recette qui ne vous appartient pas");
        }

        logger.info("Updating recipe {}", id);
        var recipe = RecipeDTOMapper.toDomain(RecipeDTOMapper.recipeDTOWithUser(recipeDTO, currentUser));
        var updatedRecipe = recipeService.updateRecipe(id, recipe, ImageMapper.toImageUpload(image));
        logger.info("Recipe {} updated", updatedRecipe);
        return "redirect:/recipes/" + updatedRecipe.id();
    }

    @GetMapping("/create-variant")
    public String pageCreateVariant(@RequestParam("id") UUID idParent, @RequestParam("title") String titleRecipeParent,
                                    @RequestParam("username") String usernameOwnerRecipeParent, Model model) {
        var recipeParent = recipeService.findById(idParent);
        var recipeParentDTO = RecipeDTOMapper.toDTO(recipeParent);
        var allAllergens = recipeService.findAllAllergens().stream()
                .map(RecipeDTOMapper::toDTO)
                .toList();
        var allIngredientNames = recipeService.findAllIngredientNames();
        var allUnitNames = recipeService.findAllUnitNames();
        var allDietaryNames = recipeService.findAllDietaryNames();
        var username = authPort.extractUsername();

        var selectedAllergenIds = recipeParentDTO.allergens().stream().map(AllergenDTO::id).toList();

        model.addAttribute("recipeBase", recipeParentDTO);
        model.addAttribute("parentId", recipeParentDTO.id());
        model.addAttribute("recipe", recipeParentDTO);
        model.addAttribute("allAllergens", allAllergens);
        model.addAttribute("allIngredientNames", allIngredientNames);
        model.addAttribute("allUnitNames", allUnitNames);
        model.addAttribute("allDietaryNames", allDietaryNames);
        model.addAttribute("selectedAllergenIds", selectedAllergenIds);
        model.addAttribute("username", username);
        model.addAttribute("formAction", "/recipes/create-variant");
        model.addAttribute("formTitle", "Créer une variante");
        return "recipes/create-variant";
    }

    @PostMapping("/create-variant")
    public String createVariant(@ModelAttribute RecipeDTO recipeDTO,
                                @RequestPart(value = "image", required = false) MultipartFile image,
                                Model model) {
        var currentUser = authPort.extractUsername();
        var dto = RecipeDTOMapper.recipeDTOWithUser(recipeDTO, currentUser);
        var hasNewImage = image != null && !image.isEmpty();
        if (!hasNewImage && recipeDTO.parentId() != null) {
            var parent = recipeService.findById(recipeDTO.parentId());
            logger.info("Adding image from parent {}\n\n\n", parent);
            dto = RecipeDTOMapper.recipeDTOWithImageUrl(dto, parent.imageUrl());
        }
        var savedRecipe = recipeService.createRecipe(RecipeDTOMapper.toDomain(dto), hasNewImage ? ImageMapper.toImageUpload(image) : null);
        return "redirect:/recipes/" + savedRecipe.id();
    }

    @PostMapping("/{id}/like")
    public String likeRecipe(@PathVariable UUID id) {
        var user = userService.getUserByUsername(authPort.extractUsername());
        recipeService.likeRecipe(user.id(), id);
        return "redirect:/recipes/" + id;
    }

    @PostMapping("/{id}/unlike")
    public String unlikeRecipe(@PathVariable UUID id) {
        var user = userService.getUserByUsername(authPort.extractUsername());
        recipeService.unlikeRecipe(user.id(), id);
        return "redirect:/recipes/" + id;
    }
}
