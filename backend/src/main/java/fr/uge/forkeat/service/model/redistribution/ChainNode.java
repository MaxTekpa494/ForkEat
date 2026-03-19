package fr.uge.forkeat.service.model.redistribution;

import java.util.UUID;

/**
 * Nœud d'une chaîne d'auteurs.
 * depth=0 : auteur direct de la recette SuperLikée ; depth croissant : ancêtres.
 */
public record ChainNode(UUID authorId, UUID recipeId, int depth) {}