package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeReportDetailsView;
import fr.uge.forkeat.service.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeReportRepository extends JpaRepository<RecipeReportEntity, UUID> {

    List<RecipeReportEntity> findByRecipeId(UUID recipeId);

    List<RecipeReportEntity> findByStatus(ReportStatus status);

    List<RecipeReportEntity> findByReporterId(UUID reporterId);

    boolean existsByRecipeIdAndReporterId(UUID recipeId, UUID reporterId);

    long countByStatus(ReportStatus status);

    // TODO : ajouter auteur recette ne peut pas modérer le signalement de sa recette
    @Query("""
        SELECT r.id AS id, rec.id AS recipeId, rec.title AS recipeTitle,
               rec.imageUrl AS recipeImageUrl,
               u.username AS reporterUsername, r.reportType AS reportType,\s
               r.justification AS justification, r.createdAt AS createdAt
        FROM RecipeReportEntity r
        JOIN r.recipe rec
        LEFT JOIN r.reporter u
        WHERE u.id <> :reporterId AND r.status = :status
   \s""")
    Page<RecipeReportDetailsView> findRecipeReportsByStatusAndNotReporterIdWithRecipeAndReporter(ReportStatus status, UUID reporterId, Pageable pageable);
}