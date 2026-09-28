package fr.uge.forkeat.service.model.recipe.projection;

import fr.uge.forkeat.service.model.PageResult;

public record AuthorRecipesPage(UserRecipeStats stats, PageResult<AuthorRecipeSummary> recipes) {}