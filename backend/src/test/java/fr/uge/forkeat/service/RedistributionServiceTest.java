package fr.uge.forkeat.service;

import fr.uge.forkeat.service.RedistributionPaymentService.AuthorPayout;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.redistribution.ChainEntry;
import fr.uge.forkeat.service.model.redistribution.ChainNode;
import fr.uge.forkeat.service.model.redistribution.EarningsRow;
import fr.uge.forkeat.service.model.redistribution.RedistributionSummary;
import fr.uge.forkeat.service.model.redistribution.UnprocessedSL;
import fr.uge.forkeat.service.model.wallet.Wallet;
import fr.uge.forkeat.service.persistence.RedistributionPersistence;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedistributionServiceTest {

    @Mock RedistributionPersistence redistributionPersistence;
    @Mock WalletPersistence walletPersistence;
    @Mock RedistributionPaymentService paymentService;
    @Mock UserIdentityPort userIdentityPort;

    RedistributionService service;

    static final String BATCH_MONTH = "2026-03";
    static final UUID REDISTRIBUTION_WALLET_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RedistributionService(redistributionPersistence, walletPersistence, paymentService,
                userIdentityPort);

        // Par défaut : cross-check OK (getTotalRedistributedForMonth retourne 0, ajusté par test)
        lenient().when(walletPersistence.getRedistributionWallet())
            .thenReturn(new Wallet(REDISTRIBUTION_WALLET_ID, UUID.randomUUID(), 100_000L, Instant.now()));
        lenient().when(redistributionPersistence.existsRedistributionFor(any(), any(), any()))
            .thenReturn(false);
    }

    // --- Helpers ---

    private UUID stubWallet(UUID authorId) {
        var walletId = UUID.randomUUID();
        lenient().when(walletPersistence.findByUserId(authorId))
            .thenReturn(Optional.of(new Wallet(walletId, authorId, 0L, Instant.now())));
        return walletId;
    }

    private UnprocessedSL sl(UUID recipeId, long redistAmount) {
        return new UnprocessedSL(UUID.randomUUID(), recipeId, redistAmount, Instant.now());
    }

    private UnprocessedSL slAt(UUID recipeId, long redistAmount, Instant date) {
        return new UnprocessedSL(UUID.randomUUID(), recipeId, redistAmount, date);
    }

    private ChainNode node(UUID authorId, UUID recipeId, int depth) {
        return new ChainNode(authorId, recipeId, depth);
    }

    @SuppressWarnings("unchecked")
    private List<AuthorPayout> capturePayouts(UUID recipeId) {
        var captor = ArgumentCaptor.forClass(List.class);
        verify(paymentService).applyPayments(captor.capture(), eq(recipeId), eq(BATCH_MONTH), eq(REDISTRIBUTION_WALLET_ID));
        return (List<AuthorPayout>) captor.getValue();
    }

    private long totalPaidOut(List<AuthorPayout> payouts) {
        return payouts.stream().mapToLong(AuthorPayout::amount).sum();
    }

    // --- Cas basiques ---

    @Nested
    @DisplayName("Cas sans SLs")
    class EmptyBatch {

        @Test
        @DisplayName("Aucun SL non traité → batch termine sans opération")
        void noUnprocessedSls_batchExitsCleanly() {
            when(redistributionPersistence.findUnprocessedSuperLikes()).thenReturn(List.of());

            service.processAllPending(BATCH_MONTH);

            verifyNoInteractions(paymentService);
            verify(redistributionPersistence, never()).markSuperLikesProcessed(any(), any());
        }
    }

    @Nested
    @DisplayName("Algorithme de répartition — recettes de base")
    class BaseRecipeDistribution {

        @Test
        @DisplayName("Recette de base, total = 60 → auteur reçoit 100%")
        void baseRecipe_fullAmountToDirectAuthor() {
            var authorId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var walletId = stubWallet(authorId);
            var sl = sl(recipeId, 60L);

            when(redistributionPersistence.findUnprocessedSuperLikes()).thenReturn(List.of(sl));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(false);
            when(redistributionPersistence.getAuthorChain(eq(recipeId), any()))
                .thenReturn(List.of(node(authorId, recipeId, 0)));
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(60L);

            service.processAllPending(BATCH_MONTH);

            var payouts = capturePayouts(recipeId);
            assertThat(payouts).hasSize(1);
            assertThat(payouts.getFirst().walletId()).isEqualTo(walletId);
            assertThat(payouts.getFirst().amount()).isEqualTo(60L);
        }

        @Test
        @DisplayName("Total = 0 → aucun paiement, SL marqué traité, pas d'erreur")
        void zeroAmount_noPayments() {
            var authorId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            stubWallet(authorId);
            var sl = sl(recipeId, 0L);

            when(redistributionPersistence.findUnprocessedSuperLikes()).thenReturn(List.of(sl));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(false);
            when(redistributionPersistence.getAuthorChain(eq(recipeId), any()))
                .thenReturn(List.of(node(authorId, recipeId, 0)));
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(0L);

            service.processAllPending(BATCH_MONTH);

            // total = 0 → distributeToChain retourne immédiatement → aucun paiement
            verifyNoInteractions(paymentService);
            verify(redistributionPersistence).markSuperLikesProcessed(List.of(sl.superLikeId()), BATCH_MONTH);
        }
    }

    @Nested
    @DisplayName("Algorithme de répartition — variantes")
    class VariantDistribution {

        @Test
        @DisplayName("Variante 1 niveau, total = 60 → 30 / 30")
        void variant1Level_evenSplit() {
            var directId = UUID.randomUUID();
            var parentId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var parentRecipeId = UUID.randomUUID();
            var directWallet = stubWallet(directId);
            var parentWallet = stubWallet(parentId);

            when(redistributionPersistence.findUnprocessedSuperLikes()).thenReturn(List.of(sl(recipeId, 60L)));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(false);
            when(redistributionPersistence.getAuthorChain(eq(recipeId), any()))
                .thenReturn(List.of(node(directId, recipeId, 0), node(parentId, parentRecipeId, 1)));
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(60L);

            service.processAllPending(BATCH_MONTH);

            var payouts = capturePayouts(recipeId);
            assertThat(totalPaidOut(payouts)).isEqualTo(60L);
            assertThat(amountFor(payouts, directWallet)).isEqualTo(30L);
            assertThat(amountFor(payouts, parentWallet)).isEqualTo(30L);
        }

        @Test
        @DisplayName("Variante 2 niveaux, total = 60 → 30 / 15 / 15")
        void variant2Levels_60cts() {
            var chain = buildChain3(60L);

            var payouts = capturePayouts(chain.recipeId());
            assertThat(totalPaidOut(payouts)).isEqualTo(60L);
            assertThat(amountFor(payouts, chain.directWallet())).isEqualTo(30L);
            assertThat(amountFor(payouts, chain.parentWallet())).isEqualTo(15L);
            assertThat(amountFor(payouts, chain.baseWallet())).isEqualTo(15L);
        }

        @Test
        @DisplayName("Variante 2 niveaux, total = 61 → 31 / 15 / 15 (centime au plus proche)")
        void variant2Levels_61cts_residualToClosest() {
            var chain = buildChain3(61L);

            var payouts = capturePayouts(chain.recipeId());
            assertThat(totalPaidOut(payouts)).isEqualTo(61L);
            assertThat(amountFor(payouts, chain.directWallet())).isEqualTo(31L);
            assertThat(amountFor(payouts, chain.parentWallet())).isEqualTo(15L);
            assertThat(amountFor(payouts, chain.baseWallet())).isEqualTo(15L);
        }

        @Test
        @DisplayName("Variante 2 niveaux, total = 63 → 32 / 16 / 15")
        void variant2Levels_63cts() {
            var chain = buildChain3(63L);

            var payouts = capturePayouts(chain.recipeId());
            assertThat(totalPaidOut(payouts)).isEqualTo(63L);
            assertThat(amountFor(payouts, chain.directWallet())).isEqualTo(32L);
            assertThat(amountFor(payouts, chain.parentWallet())).isEqualTo(16L);
            assertThat(amountFor(payouts, chain.baseWallet())).isEqualTo(15L);
        }

        // Construit une chaîne à 3 niveaux et exécute le batch
        private Chain3 buildChain3(long total) {
            var directId = UUID.randomUUID();
            var parentId = UUID.randomUUID();
            var baseId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var parentRecipeId = UUID.randomUUID();
            var baseRecipeId = UUID.randomUUID();
            var directWallet = stubWallet(directId);
            var parentWallet = stubWallet(parentId);
            var baseWallet = stubWallet(baseId);

            when(redistributionPersistence.findUnprocessedSuperLikes())
                .thenReturn(List.of(sl(recipeId, total)));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(false);
            when(redistributionPersistence.getAuthorChain(eq(recipeId), any()))
                .thenReturn(List.of(
                    node(directId, recipeId, 0),
                    node(parentId, parentRecipeId, 1),
                    node(baseId, baseRecipeId, 2)));
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(total);

            service.processAllPending(BATCH_MONTH);
            return new Chain3(recipeId, directWallet, parentWallet, baseWallet);
        }

        record Chain3(UUID recipeId, UUID directWallet, UUID parentWallet, UUID baseWallet) {}
    }

    @Nested
    @DisplayName("Auteurs sans wallet personnel")
    class FallbackToEarnings {

        @Test
        @DisplayName("Auteur sans wallet → part versée au wallet EARNINGS")
        void authorWithoutWallet_fallsBackToEarningsWallet() {
            var directId = UUID.randomUUID();
            var systemId = UUID.randomUUID(); // system_earnings, pas de wallet perso
            var recipeId = UUID.randomUUID();
            var parentRecipeId = UUID.randomUUID();
            var directWallet = stubWallet(directId);
            var earningsWalletId = UUID.randomUUID();

            when(walletPersistence.findByUserId(systemId)).thenReturn(Optional.empty());
            when(walletPersistence.getEarningsWallet())
                .thenReturn(new Wallet(earningsWalletId, UUID.randomUUID(), 0L, Instant.now()));

            when(redistributionPersistence.findUnprocessedSuperLikes()).thenReturn(List.of(sl(recipeId, 60L)));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(false);
            when(redistributionPersistence.getAuthorChain(eq(recipeId), any()))
                .thenReturn(List.of(node(directId, recipeId, 0), node(systemId, parentRecipeId, 1)));
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(60L);

            service.processAllPending(BATCH_MONTH);

            var payouts = capturePayouts(recipeId);
            assertThat(amountFor(payouts, directWallet)).isEqualTo(30L);
            assertThat(amountFor(payouts, earningsWalletId)).isEqualTo(30L);
        }
    }

    @Nested
    @DisplayName("Optimisation — chaîne inchangée vs. modifiée")
    class ChainOptimization {

        @Test
        @DisplayName("3 SLs, aucun ancêtre supprimé → une seule requête de chaîne")
        void multipleSls_noChainChange_singleChainQuery() {
            var authorId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            stubWallet(authorId);

            when(redistributionPersistence.findUnprocessedSuperLikes())
                .thenReturn(List.of(sl(recipeId, 20L), sl(recipeId, 20L), sl(recipeId, 20L)));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(false);
            when(redistributionPersistence.getAuthorChain(eq(recipeId), any()))
                .thenReturn(List.of(node(authorId, recipeId, 0)));
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(60L);

            service.processAllPending(BATCH_MONTH);

            // Une seule requête de chaîne (pas 3)
            verify(redistributionPersistence, times(1)).getAuthorChain(eq(recipeId), any());

            var payouts = capturePayouts(recipeId);
            assertThat(payouts.getFirst().amount()).isEqualTo(60L);
        }

        @Test
        @DisplayName("Ancêtre supprimé mid-période → traitement SL par SL, agrégation correcte")
        void chainChangedMidPeriod_slBySlProcessing_correctAggregation() {
            var directId = UUID.randomUUID();
            var deletedId = UUID.randomUUID(); // supprimé entre T1 et T3
            var baseId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var deletedRecipeId = UUID.randomUUID();
            var baseRecipeId = UUID.randomUUID();

            var directWallet = stubWallet(directId);
            var earningsWalletId = UUID.randomUUID();
            var baseWallet = stubWallet(baseId);
            when(walletPersistence.findByUserId(deletedId)).thenReturn(Optional.empty());
            when(walletPersistence.getEarningsWallet())
                .thenReturn(new Wallet(earningsWalletId, UUID.randomUUID(), 0L, Instant.now()));

            var t1 = Instant.parse("2026-02-01T10:00:00Z");
            var t3 = Instant.parse("2026-02-20T10:00:00Z");
            var slT1 = slAt(recipeId, 30L, t1);
            var slT3 = slAt(recipeId, 30L, t3);

            when(redistributionPersistence.findUnprocessedSuperLikes()).thenReturn(List.of(slT1, slT3));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(true);

            // SL à T1 : chaîne complète (B encore vivant)
            when(redistributionPersistence.getAuthorChain(eq(recipeId), eq(t1)))
                .thenReturn(List.of(
                    node(directId, recipeId, 0),
                    node(deletedId, deletedRecipeId, 1),
                    node(baseId, baseRecipeId, 2)));

            // SL à T3 : chaîne sans B (supprimé)
            when(redistributionPersistence.getAuthorChain(eq(recipeId), eq(t3)))
                .thenReturn(List.of(
                    node(directId, recipeId, 0),
                    node(baseId, baseRecipeId, 1)));

            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(60L);

            service.processAllPending(BATCH_MONTH);

            var payouts = capturePayouts(recipeId);
            assertThat(totalPaidOut(payouts)).isEqualTo(60L);

            // SL T1 (30 cts, chaîne A→B→C) : direct=16, system(B)=8, base=6... attendons
            // ceil(30/2)=15 → direct, ceil(15/2)=8 → deleted, 7 → base
            // SL T3 (30 cts, chaîne A→C) : direct=15, base=15
            // Agrégé : direct=15+15=30, earnings(B)=8, base=7+15=22
            assertThat(amountFor(payouts, directWallet)).isEqualTo(30L);
            assertThat(amountFor(payouts, earningsWalletId)).isEqualTo(8L);
            assertThat(amountFor(payouts, baseWallet)).isEqualTo(22L);
        }

        @Test
        @DisplayName("Invariant : sum(authorTotals) = sum(redistAmounts) après agrégation SL par SL")
        void chainChangedMidPeriod_totalIsConserved() {
            var directId = UUID.randomUUID();
            var baseId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var baseRecipeId = UUID.randomUUID();
            stubWallet(directId);
            stubWallet(baseId);

            var t1 = Instant.parse("2026-02-01T10:00:00Z");
            var t3 = Instant.parse("2026-02-20T10:00:00Z");

            when(redistributionPersistence.findUnprocessedSuperLikes())
                .thenReturn(List.of(slAt(recipeId, 30L, t1), slAt(recipeId, 30L, t3)));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(true);
            when(redistributionPersistence.getAuthorChain(eq(recipeId), any()))
                .thenReturn(List.of(node(directId, recipeId, 0), node(baseId, baseRecipeId, 1)));
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(60L);

            service.processAllPending(BATCH_MONTH);

            var payouts = capturePayouts(recipeId);
            assertThat(totalPaidOut(payouts)).isEqualTo(60L);
        }
    }

    @Nested
    @DisplayName("Groupement par recette")
    class RecipeGrouping {

        @Test
        @DisplayName("2 recettes sans lien → deux groupes indépendants")
        void twoIndependentRecipes_twoSeparateGroups() {
            var authorA = UUID.randomUUID();
            var authorB = UUID.randomUUID();
            var recipeA = UUID.randomUUID();
            var recipeB = UUID.randomUUID();
            stubWallet(authorA);
            stubWallet(authorB);

            when(redistributionPersistence.findUnprocessedSuperLikes())
                .thenReturn(List.of(sl(recipeA, 60L), sl(recipeB, 40L)));
            when(redistributionPersistence.hasChainChangedBetween(any(UUID.class), any(), any())).thenReturn(false);
            when(redistributionPersistence.getAuthorChain(eq(recipeA), any()))
                .thenReturn(List.of(node(authorA, recipeA, 0)));
            when(redistributionPersistence.getAuthorChain(eq(recipeB), any()))
                .thenReturn(List.of(node(authorB, recipeB, 0)));
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(100L);

            service.processAllPending(BATCH_MONTH);

            verify(paymentService, times(2)).applyPayments(any(), any(UUID.class), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Chaîne vide")
    class EmptyChain {

        @Test
        @DisplayName("Chaîne vide → SLs marqués traités sans paiement")
        void emptyChain_slsMarkedWithoutPayment() {
            var recipeId = UUID.randomUUID();
            var sl = sl(recipeId, 60L);

            when(redistributionPersistence.findUnprocessedSuperLikes()).thenReturn(List.of(sl));
            when(redistributionPersistence.hasChainChangedBetween(eq(recipeId), any(), any())).thenReturn(false);
            when(redistributionPersistence.getAuthorChain(eq(recipeId), any())).thenReturn(List.of());
            when(redistributionPersistence.getTotalRedistributedForMonth(BATCH_MONTH)).thenReturn(0L);

            service.processAllPending(BATCH_MONTH);

            verifyNoInteractions(paymentService);
            verify(redistributionPersistence).markSuperLikesProcessed(List.of(sl.superLikeId()), BATCH_MONTH);
        }
    }

    @Nested
    @DisplayName("Méthodes de consultation")
    class QueryMethods {

        @Test
        @DisplayName("getMyEarnings → résout l'userId via UserIdentityPort et délègue à la persistance")
        void getMyEarnings_resolvesUsernameAndDelegates() {
            var userId = UUID.randomUUID();
            var sourceId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var row = new EarningsRow("2026-03", 60L, sourceId, recipeId, "Ma recette");
            when(userIdentityPort.findIdByUsernameOrThrow("alice")).thenReturn(userId);
            when(redistributionPersistence.findEarningsByUser(userId)).thenReturn(List.of(row));

            var result = service.getUserEarnings("alice");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isEqualTo(row);
        }

        @Test
        @DisplayName("getMyEarnings → utilisateur inconnu lève ResourceNotFoundException")
        void getMyEarnings_unknownUser_throws() {
            when(userIdentityPort.findIdByUsernameOrThrow("ghost"))
                .thenThrow(new ResourceNotFoundException("User not found: ghost"));

            assertThatThrownBy(() -> service.getUserEarnings("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getMyEarnings → aucun gain retourne liste vide")
        void getMyEarnings_noEarnings_returnsEmpty() {
            var userId = UUID.randomUUID();
            when(userIdentityPort.findIdByUsernameOrThrow("bob")).thenReturn(userId);
            when(redistributionPersistence.findEarningsByUser(userId)).thenReturn(List.of());

            assertThat(service.getUserEarnings("bob")).isEmpty();
        }

        @Test
        @DisplayName("getRedistributionChain → délègue directement à la persistance")
        void getRedistributionChain_delegates() {
            var recipeId = UUID.randomUUID();
            var authorId = UUID.randomUUID();
            var entry = new ChainEntry(authorId, "alice", recipeId, "Tarte aux pommes", 30L);
            when(redistributionPersistence.findChainForRecipeAndMonth(recipeId, "2026-03"))
                .thenReturn(List.of(entry));

            var result = service.getRedistributionChain(recipeId, "2026-03");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().username()).isEqualTo("alice");
            assertThat(result.getFirst().amountCents()).isEqualTo(30L);
        }

        @Test
        @DisplayName("getRedistributionChain → recette sans données retourne liste vide")
        void getRedistributionChain_noData_returnsEmpty() {
            var recipeId = UUID.randomUUID();
            when(redistributionPersistence.findChainForRecipeAndMonth(recipeId, "2026-03"))
                .thenReturn(List.of());

            assertThat(service.getRedistributionChain(recipeId, "2026-03")).isEmpty();
        }

        @Test
        @DisplayName("getRedistributionSummary → délègue à la persistance")
        void getRedistributionSummary_delegates() {
            var sourceId = UUID.randomUUID();
            var summary = new RedistributionSummary("2026-03", sourceId, "Tarte aux pommes", 60L);
            when(redistributionPersistence.findRedistributionSummary()).thenReturn(List.of(summary));

            var result = service.getRedistributionSummary();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().batchMonth()).isEqualTo("2026-03");
            assertThat(result.getFirst().totalCents()).isEqualTo(60L);
        }
    }

    // --- Utilitaire ---

    private long amountFor(List<AuthorPayout> payouts, UUID walletId) {
        return payouts.stream()
            .filter(p -> p.walletId().equals(walletId))
            .mapToLong(AuthorPayout::amount)
            .sum();
    }
}