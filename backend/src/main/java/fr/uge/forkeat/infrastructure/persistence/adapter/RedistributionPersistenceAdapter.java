package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jRedistributionRepository;
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
                p.recipeId(),
                p.redistAmount(),
                p.date()
            ))
            .toList();
    }

    @Override
    public List<ChainNode> getAuthorChain(String recipeId, Instant atDate) {
        return repository.getAuthorChain(recipeId, atDate.atOffset(ZoneOffset.UTC))
            .stream()
            .map(p -> new ChainNode(p.authorId(), p.recipeId(), p.depth()))
            .sorted(Comparator.comparingInt(ChainNode::depth))
            .toList();
    }

    @Override
    public boolean hasChainChangedBetween(String recipeId, Instant oldest, Instant newest) {
        return repository.hasChainChangedBetween(
            recipeId,
            oldest.atOffset(ZoneOffset.UTC),
            newest.atOffset(ZoneOffset.UTC)
        );
    }

    @Override
    public boolean existsRedistributionFor(String authorId, String sourceRecipeId, String batchMonth) {
        return repository.existsRedistributionFor(authorId, sourceRecipeId, batchMonth);
    }

    @Override
    public void saveAuthorRedistribution(String authorId, String authorRecipeId,
                                         String sourceRecipeId, long amountCents, String batchMonth) {
        repository.saveAuthorRedistribution(authorId, authorRecipeId, sourceRecipeId, amountCents, batchMonth);
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
}