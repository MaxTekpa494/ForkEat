package fr.uge.forkeat.service.model.redistribution;

import java.util.UUID;

/** Un nœud dans la chaîne de redistribution d'une recette pour un mois donné. */
public record ChainEntry(UUID authorId, String username, UUID recipeId,
                         String recipeTitle, long amountCents) {}