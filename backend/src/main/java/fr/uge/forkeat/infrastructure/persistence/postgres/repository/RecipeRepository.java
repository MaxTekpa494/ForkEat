package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<RecipeEntity, UUID> {

    Optional<RecipeEntity> findBySourceAndExternalId(String source, String externalId);

    boolean existsBySourceAndExternalId(String source, String externalId);

    List<RecipeEntity> findBySource(String source);

    List<RecipeEntity> findByStatus(RecipeStatus status);

    Page<RecipeEntity> findByStatus(RecipeStatus status, Pageable pageable);

    List<RecipeEntity> findByAuthorId(UUID authorId);

    long countBySource(String source);

    List<RecipeEntity> findByTitleContainingIgnoreCase(String title);

    boolean existsByTitle(String title);

    List<RecipeEntity> findByStatusIn(List<RecipeStatus> statuses);

    long countByStatus(RecipeStatus status);

    long countByAuthorId(UUID authorId);

    List<RecipeEntity> findByAuthorIdAndStatus(UUID authorId, RecipeStatus status);
}
