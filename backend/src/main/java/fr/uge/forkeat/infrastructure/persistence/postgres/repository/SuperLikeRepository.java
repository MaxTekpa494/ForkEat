package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.SuperLikeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.SuperLikeHistoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SuperLikeRepository extends JpaRepository<SuperLikeEntity, UUID> {

    boolean existsByRecipeIdAndUserId(UUID recipeId, UUID userId);

    @Query("SELECT COUNT(s) FROM SuperLikeEntity s WHERE s.userId = :userId AND s.promotionId = :promotionId AND s.isBonusFree = false")
    int countPaidByUserAndPromotion(@Param("userId") UUID userId, @Param("promotionId") UUID promotionId);

    @Query("SELECT COUNT(s) FROM SuperLikeEntity s WHERE s.userId = :userId AND s.promotionId = :promotionId AND s.isBonusFree = true")
    int countFreeByUserAndPromotion(@Param("userId") UUID userId, @Param("promotionId") UUID promotionId);

    /** Historique des super-likes d'un utilisateur, du plus récent au plus ancien. */
    @Query("SELECT s FROM SuperLikeEntity s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<SuperLikeEntity> findByUserId(@Param("userId") UUID userId);

    /**
     * Historique enrichi avec titre de recette et nom de promotion (pour le reporting financier).
     * Utilisé pour les endpoints de reporting accessible aux utilisateurs et à l'admin.
     */
    @Query(nativeQuery = true, value = """
            SELECT sl.id            AS id,
                   sl.recipe_id    AS recipeId,
                   r.title         AS recipeTitle,
                   sl.amount       AS amountCents,
                   sl.is_bonus_free AS isBonusFree,
                   p.name          AS promotionName,
                   sl.created_at   AS createdAt
            FROM super_likes sl
            LEFT JOIN recipes    r ON sl.recipe_id    = r.id
            LEFT JOIN promotions p ON sl.promotion_id = p.id
            WHERE sl.user_id = :userId
            ORDER BY sl.created_at DESC
            """)
    List<SuperLikeHistoryView> findHistoryByUserId(@Param("userId") UUID userId);
}
