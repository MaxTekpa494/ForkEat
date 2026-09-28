package fr.uge.forkeat.service;

import fr.uge.forkeat.service.RedistributionPaymentService.AuthorPayout;
import fr.uge.forkeat.service.model.redistribution.ChainEntry;
import fr.uge.forkeat.service.model.redistribution.ChainNode;
import fr.uge.forkeat.service.model.redistribution.EarningsRow;
import fr.uge.forkeat.service.model.redistribution.RedistributionSummary;
import fr.uge.forkeat.service.model.redistribution.UnprocessedSL;
import fr.uge.forkeat.service.model.wallet.Wallet;
import fr.uge.forkeat.service.persistence.RedistributionPersistence;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RedistributionService {

    private static final Logger logger = LoggerFactory.getLogger(RedistributionService.class);

    private final RedistributionPersistence redistributionPersistence;
    private final WalletPersistence walletPersistence;
    private final RedistributionPaymentService paymentService;
    private final UserIdentityPort userIdentityPort;

    public RedistributionService(RedistributionPersistence redistributionPersistence,
                                 WalletPersistence walletPersistence,
                                 RedistributionPaymentService paymentService,
                                 UserIdentityPort userIdentityPort) {
        this.redistributionPersistence = redistributionPersistence;
        this.walletPersistence = walletPersistence;
        this.paymentService = paymentService;
        this.userIdentityPort = userIdentityPort;
    }

    public void processAllPending(String batchMonth) {
        logger.info("Starting redistribution batch for month: {}", batchMonth);
        var allSls = redistributionPersistence.findUnprocessedSuperLikes();
        if (allSls.isEmpty()) {
            logger.info("No unprocessed super-likes found. Batch completed with no operations.");
            return;
        }
        var groupedByRecipe = allSls.stream().collect(Collectors.groupingBy(UnprocessedSL::recipeId));
        var redistributionWalletId = walletPersistence.getRedistributionWallet().id();
        long totalPaidOut = 0L;
        for (var entry : groupedByRecipe.entrySet()) {
            var recipeId = entry.getKey();
            var sls = entry.getValue();

            // 1. Calcul des parts (avant le versement atomique)
            var authorTotals = computeAuthorTotals(recipeId, sls);
            if (authorTotals.isEmpty()) {
                logger.warn("No author chain found for recipe {}. Marking SLs processed.", recipeId);
                redistributionPersistence.markSuperLikesProcessed(
                    sls.stream().map(UnprocessedSL::superLikeId).toList(), batchMonth);
                continue;
            }

            // 2. Versements atomiques
            var payouts = new ArrayList<>(authorTotals.values());
            paymentService.applyPayments(payouts, recipeId, batchMonth, redistributionWalletId);

            // 3. Enregistrement des redistributions et marquage des SLs traités
            for (var payout : payouts) {
                if (!redistributionPersistence.existsRedistributionFor(payout.authorId(), recipeId, batchMonth)) {
                    redistributionPersistence.saveAuthorRedistribution(
                        payout.authorId(), payout.authorRecipeId(), recipeId, payout.amount(), batchMonth);
                }
            }
            redistributionPersistence.markSuperLikesProcessed(
                sls.stream().map(UnprocessedSL::superLikeId).toList(), batchMonth);
            var recipePaidOut = payouts.stream().mapToLong(AuthorPayout::amount).sum();
            totalPaidOut += recipePaidOut;
            logger.info("Recipe {} processed: {} SLs, {} cts to {} authors",
                recipeId, sls.size(), recipePaidOut, payouts.size());
        }

        // 4. Nettoyage des nœuds supprimés isolables (une seule fois, en fin de batch)
        redistributionPersistence.cleanupAfterBatch();

        // 5. Cross-check
        var registeredTotal = redistributionPersistence.getTotalRedistributedForMonth(batchMonth);
        if (registeredTotal != totalPaidOut) {
            logger.error("CROSS-CHECK FAILED for batch {}: paid {} cts, REDISTRIBUTION_RECEIVED = {} cts",
                batchMonth, totalPaidOut, registeredTotal);
        } else {
            logger.info("Cross-check OK: {} cts redistributed for month {}", totalPaidOut, batchMonth);
        }
    }

    public List<EarningsRow> getUserEarnings(String username) {
        var userId = userIdentityPort.findIdByUsernameOrThrow(username);
        return redistributionPersistence.findEarningsByUser(userId);
    }

    public List<ChainEntry> getRedistributionChain(UUID recipeId, String batchMonth) {
        return redistributionPersistence.findChainForRecipeAndMonth(recipeId, batchMonth);
    }

    public List<RedistributionSummary> getRedistributionSummary() {
        return redistributionPersistence.findRedistributionSummary();
    }

    /**
     * Calcule les montants à verser par auteur pour un groupe de SLs sur une recette.
     * Optimisation : si la chaîne n'a pas changé entre le plus ancien et le plus récent SL,
     * on fait une seule requête de chaîne sur le total. Sinon, on traite SL par SL.
     */
    private Map<UUID, AuthorPayout> computeAuthorTotals(UUID recipeId, List<UnprocessedSL> sls) {
        var oldest = sls.stream().map(UnprocessedSL::date).min(Comparator.naturalOrder()).orElseThrow();
        var newest = sls.stream().map(UnprocessedSL::date).max(Comparator.naturalOrder()).orElseThrow();

        var totals = new LinkedHashMap<UUID, AuthorPayout>();

        if (!redistributionPersistence.hasChainChangedBetween(recipeId, oldest, newest)) {
            // Cas standard : une seule requête de chaîne sur le total agrégé
            var chain = redistributionPersistence.getAuthorChain(recipeId, oldest);
            var total = sls.stream().mapToLong(UnprocessedSL::redistAmount).sum();
            distributeToChain(chain, total, totals);
        } else {
            // Cas edge (ancêtre supprimé mid-période) : traitement SL par SL
            for (var sl : sls) {
                var chain = redistributionPersistence.getAuthorChain(recipeId, sl.date());
                var slShare = new LinkedHashMap<UUID, AuthorPayout>();
                distributeToChain(chain, sl.redistAmount(), slShare);
                slShare.forEach((authorId, payout) ->
                    totals.merge(authorId, payout, (a, b) ->
                        new AuthorPayout(a.authorId(), a.walletId(), a.authorRecipeId(), a.amount() + b.amount())));
            }
        }

        return totals;
    }

    /**
     * Distribue un montant total sur une chaîne d'auteurs (ceil au plus proche, reste au dernier).
     * Accumule les résultats dans la map passée en paramètre.
     */
    private void distributeToChain(List<ChainNode> chain, long total, Map<UUID, AuthorPayout> accumulator) {
        if (chain.isEmpty() || total == 0) return;

        long remaining = total;
        for (int i = 0; i < chain.size() - 1; i++) {
            long share = (remaining + 1) / 2; // ceil : résidu au plus proche
            var node = chain.get(i);
            var walletId = resolveWalletId(node.authorId());
            accumulator.merge(node.authorId(),
                new AuthorPayout(node.authorId(), walletId, node.recipeId(), share),
                (a, b) -> new AuthorPayout(a.authorId(), a.walletId(), a.authorRecipeId(), a.amount() + b.amount()));
            remaining -= share;
        }
        var last = chain.getLast();
        var walletId = resolveWalletId(last.authorId());
        accumulator.merge(last.authorId(),
            new AuthorPayout(last.authorId(), walletId, last.recipeId(), remaining),
            (a, b) -> new AuthorPayout(a.authorId(), a.walletId(), a.authorRecipeId(), a.amount() + b.amount()));
    }

    /**
     * Résout le walletId à partir de l'authorId.
     * Si l'auteur n'a pas de wallet (ex: system_earnings), utilise le wallet EARNINGS plateforme.
     */
    private UUID resolveWalletId(UUID authorId) {
        return walletPersistence.findByUserId(authorId)
            .map(Wallet::id)
            .orElseGet(() -> walletPersistence.getEarningsWallet().id());
    }
}