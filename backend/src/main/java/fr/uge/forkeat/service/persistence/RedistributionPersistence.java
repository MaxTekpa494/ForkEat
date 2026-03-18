package fr.uge.forkeat.service.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RedistributionPersistence {

    /** Un SuperLike non encore redistribué. */
    record UnprocessedSL(UUID superLikeId, String recipeId, long redistAmount, Instant date) {}

    /**
     * Nœud d'une chaîne d'auteurs.
     * depth=0 : auteur direct de la recette SuperLikée ; depth croissant : ancêtres.
     */
    record ChainNode(String authorId, String recipeId, int depth) {}

    /** Retourne tous les SuperLikes non encore redistribués, un par ligne. */
    List<UnprocessedSL> findUnprocessedSuperLikes();

    /**
     * Retourne la chaîne d'auteurs effective pour une recette à la date du SuperLike.
     * Les ancêtres supprimés APRÈS atDate sont inclus (ils étaient vivants au moment du SL).
     * Résultat trié du plus proche (depth=0) au plus éloigné.
     */
    List<ChainNode> getAuthorChain(String recipeId, Instant atDate);

    /**
     * Retourne true si la chaîne d'auteurs de cette recette a changé
     * (suppression d'un ancêtre) entre les deux dates données.
     * Permet de décider si les SuperLikes doivent être traités individuellement.
     */
    boolean hasChainChangedBetween(String recipeId, Instant oldest, Instant newest);

    /**
     * Retourne true si une redistribution a déjà été enregistrée pour cet auteur,
     * cette recette source et ce mois. Utilisé par le service pour l'idempotence.
     */
    boolean existsRedistributionFor(String authorId, String sourceRecipeId, String batchMonth);

    /**
     * Enregistre la redistribution reçue par un auteur pour un mois donné.
     * sourceRecipeId : recette qui a reçu les SuperLikes à l'origine de ce versement.
     * authorRecipeId : recette publiée par l'auteur bénéficiaire.
     */
    void saveAuthorRedistribution(String authorId, String authorRecipeId,
                                  String sourceRecipeId, long amountCents, String batchMonth);

    /**
     * Marque les SuperLikes comme traités pour ce mois.
     * Doit être appelé en dernier pour garantir l'idempotence du batch.
     */
    void markSuperLikesProcessed(List<UUID> superLikeIds, String batchMonth);

    /**
     * Nettoie les données intermédiaires devenues inutiles après traitement complet.
     * À appeler une seule fois, en toute fin de batch.
     */
    void cleanupAfterBatch();

    /** Retourne la somme totale redistribuée pour un mois — utilisé pour le cross-check. */
    long getTotalRedistributedForMonth(String batchMonth);
}