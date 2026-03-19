package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.EarningsRowProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RedistributionChainRowProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RedistributionSummaryProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jRedistributionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedistributionPersistenceAdapterTest {

    @Mock
    private Neo4jRedistributionRepository repository;

    @InjectMocks
    private RedistributionPersistenceAdapter adapter;

    @Nested
    @DisplayName("findEarningsByUser")
    class FindEarningsByUser {

        @Test
        @DisplayName("convertit l'UUID en String pour le repo et mappe les projections en EarningsRow")
        void mapsProjectionsToEarningsRow() {
            var userId = UUID.randomUUID();
            var sourceId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var projection = new EarningsRowProjection("2026-03", 45L, sourceId.toString(), recipeId.toString(), "Tarte aux pommes");
            when(repository.findEarningsByUser(userId.toString())).thenReturn(List.of(projection));

            var result = adapter.findEarningsByUser(userId);

            assertThat(result).hasSize(1);
            var row = result.getFirst();
            assertThat(row.batchMonth()).isEqualTo("2026-03");
            assertThat(row.amountCents()).isEqualTo(45L);
            assertThat(row.sourceRecipeId()).isEqualTo(sourceId);
            assertThat(row.recipeId()).isEqualTo(recipeId);
            assertThat(row.recipeTitle()).isEqualTo("Tarte aux pommes");
        }

        @Test
        @DisplayName("retourne liste vide si l'utilisateur n'a aucun gain")
        void returnsEmptyWhenNoEarnings() {
            var userId = UUID.randomUUID();
            when(repository.findEarningsByUser(userId.toString())).thenReturn(List.of());

            assertThat(adapter.findEarningsByUser(userId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findChainForRecipeAndMonth")
    class FindChainForRecipeAndMonth {

        @Test
        @DisplayName("convertit les UUIDs et mappe les projections en ChainEntry")
        void mapsProjectionsToChainEntry() {
            var recipeId = UUID.randomUUID();
            var authorId = UUID.randomUUID();
            var authorRecipeId = UUID.randomUUID();
            var projection = new RedistributionChainRowProjection(
                authorId.toString(), "alice", authorRecipeId.toString(), "Pâte brisée", 30L);
            when(repository.findChainForRecipeAndMonth(recipeId.toString(), "2026-03"))
                .thenReturn(List.of(projection));

            var result = adapter.findChainForRecipeAndMonth(recipeId, "2026-03");

            assertThat(result).hasSize(1);
            var entry = result.getFirst();
            assertThat(entry.authorId()).isEqualTo(authorId);
            assertThat(entry.username()).isEqualTo("alice");
            assertThat(entry.recipeId()).isEqualTo(authorRecipeId);
            assertThat(entry.recipeTitle()).isEqualTo("Pâte brisée");
            assertThat(entry.amountCents()).isEqualTo(30L);
        }

        @Test
        @DisplayName("retourne liste vide si aucune donnée pour ce mois")
        void returnsEmptyWhenNoData() {
            var recipeId = UUID.randomUUID();
            when(repository.findChainForRecipeAndMonth(recipeId.toString(), "2026-03"))
                .thenReturn(List.of());

            assertThat(adapter.findChainForRecipeAndMonth(recipeId, "2026-03")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRedistributionSummary")
    class FindRedistributionSummary {

        @Test
        @DisplayName("mappe les projections en RedistributionSummary avec conversion UUID")
        void mapsProjectionsToRedistributionSummary() {
            var sourceId = UUID.randomUUID();
            var projection = new RedistributionSummaryProjection("2026-03", sourceId.toString(), "Quiche lorraine", 90L);
            when(repository.findRedistributionSummary()).thenReturn(List.of(projection));

            var result = adapter.findRedistributionSummary();

            assertThat(result).hasSize(1);
            var summary = result.getFirst();
            assertThat(summary.batchMonth()).isEqualTo("2026-03");
            assertThat(summary.sourceRecipeId()).isEqualTo(sourceId);
            assertThat(summary.sourceRecipeTitle()).isEqualTo("Quiche lorraine");
            assertThat(summary.totalCents()).isEqualTo(90L);
        }

        @Test
        @DisplayName("plusieurs mois — tous les enregistrements sont retournés")
        void returnsAllSummaries() {
            var projections = List.of(
                new RedistributionSummaryProjection("2026-03", UUID.randomUUID().toString(), "Recette A", 60L),
                new RedistributionSummaryProjection("2026-02", UUID.randomUUID().toString(), "Recette B", 30L)
            );
            when(repository.findRedistributionSummary()).thenReturn(projections);

            assertThat(adapter.findRedistributionSummary()).hasSize(2);
        }

        @Test
        @DisplayName("retourne liste vide si aucune redistribution effectuée")
        void returnsEmptyWhenNoRedistributions() {
            when(repository.findRedistributionSummary()).thenReturn(List.of());

            assertThat(adapter.findRedistributionSummary()).isEmpty();
        }
    }
}