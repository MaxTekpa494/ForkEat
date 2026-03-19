package fr.uge.forkeat.service.model.redistribution;

import java.util.UUID;

/** Un gain de redistribution reçu par un auteur. */
public record EarningsRow(String batchMonth, long amountCents, UUID sourceRecipeId,
                          UUID recipeId, String recipeTitle) {}