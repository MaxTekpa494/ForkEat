package fr.uge.forkeat.presentation.dto.redistribution;

public record EarningsDetailDTO(
        String recipeId,
        String recipeTitle,
        long amountCents,
        String sourceRecipeId
) {}