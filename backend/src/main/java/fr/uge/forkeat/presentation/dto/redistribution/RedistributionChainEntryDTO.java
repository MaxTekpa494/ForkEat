package fr.uge.forkeat.presentation.dto.redistribution;

public record RedistributionChainEntryDTO(
        String authorId,
        String username,
        String recipeId,
        String recipeTitle,
        long amountCents
) {}