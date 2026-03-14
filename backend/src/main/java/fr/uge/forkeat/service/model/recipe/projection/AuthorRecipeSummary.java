package fr.uge.forkeat.service.model.recipe.projection;

import fr.uge.forkeat.service.model.recipe.RecipeStatus;

public record AuthorRecipeSummary(RecipeSummary summary, RecipeStatus status, RecipeRejectionInfo rejectionInfo) {}