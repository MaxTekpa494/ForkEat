package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jRedistributionRepository;
import fr.uge.forkeat.service.model.redistribution.ChainEntry;
import fr.uge.forkeat.service.model.redistribution.ChainNode;
import fr.uge.forkeat.service.model.redistribution.EarningsRow;
import fr.uge.forkeat.service.model.redistribution.RedistributionSummary;
import fr.uge.forkeat.service.model.redistribution.UnprocessedSL;
import fr.uge.forkeat.service.persistence.RedistributionPersistence;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class RedistributionPersistenceAdapter implements RedistributionPersistence {

    private final Neo4jRedistributionRepository repository;

    public RedistributionPersistenceAdapter(Neo4jRedistributionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UnprocessedSL> findUnprocessedSuperLikes() {
        return repository.findUnprocessedSuperLikes().stream()
            .map(p -> new UnprocessedSL(
                UUID.fromString(p.superLikeId()),
                UUID.fromString(p.recipeId()),
                p.redistAmount(),
                p.date()
            ))
            .toList();
    }

    @Override
    public List<ChainNode> getAuthorChain(UUID recipeId, Instant atDate) {
        return repository.getAuthorChain(recipeId.toString(), atDate.atOffset(ZoneOffset.UTC))
            .stream()
            .map(p -> new ChainNode(UUID.fromString(p.authorId()), UUID.fromString(p.recipeId()), p.depth()))
            .sorted(Comparator.comparingInt(ChainNode::depth))
            .toList();
    }

    @Override
    public boolean hasChainChangedBetween(UUID recipeId, Instant oldest, Instant newest) {
        return repository.hasChainChangedBetween(
            recipeId.toString(),
            oldest.atOffset(ZoneOffset.UTC),
            newest.atOffset(ZoneOffset.UTC)
        );
    }

    @Override
    public boolean existsRedistributionFor(UUID authorId, UUID sourceRecipeId, String batchMonth) {
        return repository.existsRedistributionFor(authorId.toString(), sourceRecipeId.toString(), batchMonth);
    }

    @Override
    public void saveAuthorRedistribution(UUID authorId, UUID authorRecipeId,
                                         UUID sourceRecipeId, long amountCents, String batchMonth) {
        repository.saveAuthorRedistribution(
            authorId.toString(), authorRecipeId.toString(),
            sourceRecipeId.toString(), amountCents, batchMonth);
    }

    @Override
    public void markSuperLikesProcessed(List<UUID> superLikeIds, String batchMonth) {
        if (superLikeIds.isEmpty()) return;
        var ids = superLikeIds.stream().map(UUID::toString).toList();
        repository.markSuperLikesProcessed(ids, batchMonth);
    }

    @Override
    public void cleanupAfterBatch() {
        repository.cleanupAfterBatch();
    }

    @Override
    public long getTotalRedistributedForMonth(String batchMonth) {
        return repository.getTotalRedistributedForMonth(batchMonth);
    }

    @Override
    public List<EarningsRow> findEarningsByUser(UUID userId) {
        return repository.findEarningsByUser(userId.toString()).stream()
            .map(p -> new EarningsRow(
                p.batchMonth(),
                p.amountCents(),
                UUID.fromString(p.sourceRecipeId()),
                UUID.fromString(p.recipeId()),
                p.recipeTitle()))
            .toList();
    }

    @Override
    public List<ChainEntry> findChainForRecipeAndMonth(UUID recipeId, String batchMonth) {
        return repository.findChainForRecipeAndMonth(recipeId.toString(), batchMonth).stream()
            .map(p -> new ChainEntry(
                UUID.fromString(p.authorId()),
                p.username(),
                UUID.fromString(p.recipeId()),
                p.recipeTitle(),
                p.amountCents()))
            .toList();
    }

    @Override
    public List<RedistributionSummary> findRedistributionSummary() {
        return repository.findRedistributionSummary().stream()
            .map(p -> new RedistributionSummary(
                p.batchMonth(),
                UUID.fromString(p.sourceRecipeId()),
                p.sourceRecipeTitle(),
                p.totalCents()))
            .toList();
    }
}