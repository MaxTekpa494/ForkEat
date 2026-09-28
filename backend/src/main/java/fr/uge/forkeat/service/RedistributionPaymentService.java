package fr.uge.forkeat.service;

import fr.uge.forkeat.service.model.transaction.Transaction;
import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.persistence.PlatformWalletPersistence;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Applique les versements de redistribution de façon atomique pour un groupe de recette.
 * Ce service est séparé de RedistributionService uniquement pour permettre à Spring AOP
 * d'intercepter l'annotation @Transactional (un bean ne peut pas s'auto-appeler via le proxy).
 */
@Service
public class RedistributionPaymentService {

    // Montants agrégés à verser par auteur pour un groupe de SLs sur une recette.
    public record AuthorPayout(UUID authorId, UUID walletId, UUID authorRecipeId, long amount) {}
    private static final Logger logger = LoggerFactory.getLogger(RedistributionPaymentService.class);

    private final WalletPersistence walletPersistence;
    private final PlatformWalletPersistence platformWalletPersistence;

    public RedistributionPaymentService(WalletPersistence walletPersistence,
                                        PlatformWalletPersistence platformWalletPersistence) {
        this.walletPersistence = walletPersistence;
        this.platformWalletPersistence = platformWalletPersistence;
    }

    /**
     * Applique les versements pour un groupe de recette de façon atomique.
     * Acquiert un verrou sur le wallet de redistribution pour prévenir les runs concurrents.
     * Idempotent : les versements déjà effectués lors d'un run précédent sont ignorés.
     */
    @Transactional
    public void applyPayments(List<AuthorPayout> payouts, UUID recipeId,
                              String batchMonth, UUID redistributionWalletId) {
        platformWalletPersistence.findByTypeWithLock(PlatformWalletType.REDISTRIBUTION);
        var earningsWalletId = walletPersistence.getEarningsWallet().id();
        logger.info("[applyPayments] recipe={} month={} redistWallet={} earningsWallet={} payouts={}",
            recipeId, batchMonth, redistributionWalletId, earningsWalletId, payouts.size());

        for (var payout : payouts) {
            var deterministicId = deterministicPaymentId(recipeId.toString(), batchMonth, payout.walletId().toString());
            logger.info("[applyPayments] payout author={} walletId={} amount={} deterministicId={}",
                payout.authorId(), payout.walletId(), payout.amount(), deterministicId);

            if (walletPersistence.findTransactionById(deterministicId).isPresent()) {
                logger.info("[applyPayments] SKIP (idempotence) deterministicId={}", deterministicId);
                continue;
            }

            walletPersistence.decrementBalanceById(redistributionWalletId, payout.amount());
            walletPersistence.incrementBalanceById(payout.walletId(), payout.amount());
            walletPersistence.saveTransaction(new Transaction(
                deterministicId,
                redistributionWalletId,
                payout.walletId(),
                payout.amount(),
                TransactionType.REDISTRIBUTION,
                Instant.now(),
                null,
                TransactionStatus.SUCCEEDED
            ));
            logger.info("[applyPayments] platform log REDISTRIBUTION -{} cts (recipe={})", payout.amount(), recipeId);
            platformWalletPersistence.recordTransaction(
                PlatformWalletType.REDISTRIBUTION,
                -payout.amount(),
                "MONTHLY_REDISTRIBUTION",
                recipeId
            );
            if (payout.walletId().equals(earningsWalletId)) {
                logger.info("[applyPayments] platform log EARNINGS +{} cts (fallback, recipe={})", payout.amount(), recipeId);
                platformWalletPersistence.recordTransaction(
                    PlatformWalletType.EARNINGS,
                    payout.amount(),
                    "MONTHLY_REDISTRIBUTION",
                    recipeId
                );
            }
        }
    }

    /**
     * Génère un identifiant déterministe pour un versement.
     * Garantit l'idempotence : deux runs sur la même (recette, mois, wallet) produisent le même identifiant.
     */
    private UUID deterministicPaymentId(String recipeId, String batchMonth, String walletId) {
        return UUID.nameUUIDFromBytes(
            (recipeId + "#" + batchMonth + "#" + walletId).getBytes(StandardCharsets.UTF_8));
    }
}