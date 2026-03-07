package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.service.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}