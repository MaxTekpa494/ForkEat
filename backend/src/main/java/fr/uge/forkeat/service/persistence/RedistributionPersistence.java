package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.redistribution.ChainEntry;
import fr.uge.forkeat.service.model.redistribution.ChainNode;
import fr.uge.forkeat.service.model.redistribution.EarningsRow;
import fr.uge.forkeat.service.model.redistribution.RedistributionSummary;
import fr.uge.forkeat.service.model.redistribution.UnprocessedSL;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RedistributionPersistence {

    /** Retourne tous les SuperLikes non encore redistribués, un par ligne. */
    List<UnprocessedSL> findUnprocessedSuperLikes();

    /**
     * Retourne la chaîne d'auteurs effective pour une recette à la date du SuperLike.
     * Les ancêtres supprimés APRÈS atDate sont inclus (ils étaient vivants au moment du SL).
     * Résultat trié du plus proche (depth=0) au plus éloigné.
     */
    List<ChainNode> getAuthorChain(UUID recipeId, Instant atDate);

    /**
     * Retourne true si la chaîne d'auteurs de cette recette a changé
     * (suppression d'un ancêtre) entre les deux dates données.
     */
    boolean hasChainChangedBetween(UUID recipeId, Instant oldest, Instant newest);

    /**
     * Retourne true si une redistribution a déjà été enregistrée pour cet auteur,
     * cette recette source et ce mois.
     */
    boolean existsRedistributionFor(UUID authorId, UUID sourceRecipeId, String batchMonth);

    /**
     * Enregistre la redistribution reçue par un auteur pour un mois donné.
     */
    void saveAuthorRedistribution(UUID authorId, UUID authorRecipeId,
                                  UUID sourceRecipeId, long amountCents, String batchMonth);

    /**
     * Marque les SuperLikes comme traités pour ce mois.
     * Doit être appelé en dernier pour garantir l'idempotence du batch.
     */
    void markSuperLikesProcessed(List<UUID> superLikeIds, String batchMonth);

    /** Nettoie les données intermédiaires devenues inutiles après traitement complet. */
    void cleanupAfterBatch();

    /** Retourne la somme totale redistribuée pour un mois — utilisé pour le cross-check. */
    long getTotalRedistributedForMonth(String batchMonth);

    /** Retourne tous les gains de redistribution d'un utilisateur, triés par mois DESC. */
    List<EarningsRow> findEarningsByUser(UUID userId);

    /** Retourne la chaîne de redistribution pour une recette source et un mois. */
    List<ChainEntry> findChainForRecipeAndMonth(UUID recipeId, String batchMonth);

    /** Retourne le récapitulatif de toutes les redistributions (admin), trié par mois DESC. */
    List<RedistributionSummary> findRedistributionSummary();
}