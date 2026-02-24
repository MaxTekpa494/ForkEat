package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.SuperLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SuperLikeRepository extends JpaRepository<SuperLikeEntity, UUID> {

    boolean existsByRecipeIdAndUserId(UUID recipeId, UUID userId);
}
