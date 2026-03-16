package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.AuthorRecipeSummaryView;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeBaseSummaryView;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeStatusCount;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeSummaryView;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<RecipeEntity, UUID> {

    List<RecipeEntity> findByStatus(RecipeStatus status);

    Page<RecipeEntity> findByStatus(RecipeStatus status, Pageable pageable);

    List<RecipeEntity> findByAuthorId(UUID authorId);

    List<RecipeEntity> findByAuthorUsername(String authorUsername);


    List<RecipeEntity> findByTitleContainingIgnoreCase(String title);

    boolean existsByTitle(String title);

    List<RecipeEntity> findByStatusIn(List<RecipeStatus> statuses);

    long countByStatus(RecipeStatus status);

    long countByAuthorId(UUID authorId);

    List<RecipeEntity> findByAuthorIdAndStatus(UUID authorId, RecipeStatus status);

    @Query("""
            SELECT r.id AS id,
                   r.title AS title,
                   r.summary AS summary,
                   r.imageUrl AS imageUrl,
                   r.preparationMinutes AS preparationMinutes,
                   r.createdAt AS createdAt,
                   u.username AS authorUsername
            FROM RecipeEntity r
            LEFT JOIN r.ingredients ri
            LEFT JOIN ri.ingredient i
            LEFT JOIN r.author u
            WHERE r.status = :status
            AND (
                COALESCE(:search, '') = ''
                OR function('ts_match', CONCAT(r.title, ' ', COALESCE(r.summary, '')), :search) = true
                OR function('ts_match', COALESCE(i.name, ''), :search) = true
            )
            AND NOT EXISTS (
                SELECT 1 FROM RecipeAllergenEntity ra
                JOIN ra.allergen a
                WHERE ra.recipe = r
                AND a.name IN :allergens
            )
            GROUP BY r.id, u.username
            ORDER BY
                CASE
                    WHEN COALESCE(:search, '') != '' AND function('ts_match', r.title, :search) = true THEN 1
                    ELSE 2
                END,
                CASE
                    WHEN COALESCE(:search, '') != '' THEN function('ts_rank', CONCAT(r.title, ' ', COALESCE(r.summary, '')), :search)
                    ELSE 0
                END DESC,
                r.createdAt DESC
            """)
    Page<RecipeSummaryView> searchRecipes(
            @Param("status") RecipeStatus status,
            @Param("search") String search,
            @Param("allergens") List<String> allergens,
            Pageable pageable
    );

    Page<RecipeBaseSummaryView> findByAuthorUsernameAndStatus(String username, RecipeStatus status, Pageable pageable);

    @Query("""
            SELECT r.id AS id,
                   r.title AS title,
                   r.summary AS summary,
                   r.imageUrl AS imageUrl,
                   r.preparationMinutes AS preparationMinutes,
                   r.createdAt AS createdAt,
                   u.username AS authorUsername
            FROM RecipeEntity r
            JOIN r.author u
            WHERE r.id IN :ids
            """)
    List<RecipeSummaryView> findSummariesByIds(@Param("ids") List<UUID> ids);

    @Query(value = """
            SELECT r.id, r.title, r.summary, r.image_url AS imageUrl,
                   r.preparation_minutes AS preparationMinutes, r.created_at AS createdAt,
                   u.username AS authorUsername, r.status::text AS status,
                   latest_ma.justification, latest_ma.created_at AS rejectedAt
            FROM recipes r
            JOIN users u ON u.id = r.author_id
            LEFT JOIN LATERAL (
                SELECT justification, created_at
                FROM recipe_moderation_actions
                WHERE recipe_id = r.id AND action_type = 'REJECTED'
                ORDER BY created_at DESC
                LIMIT 1
            ) latest_ma ON true
            WHERE r.author_id = :authorId AND r.status = CAST(:status AS recipe_status)
            ORDER BY r.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM recipes WHERE author_id = :authorId AND status = CAST(:status AS recipe_status)
            """,
            nativeQuery = true)
    Page<AuthorRecipeSummaryView> findRecipeSummariesByAuthorIdAndStatus(
            @Param("authorId") UUID authorId,
            @Param("status") String status,
            Pageable pageable);

    @Query(value = "SELECT status::text AS status, COUNT(id) AS count FROM recipes WHERE author_id = :authorId GROUP BY status",
            nativeQuery = true)
    List<RecipeStatusCount> countByAuthorIdGroupByStatus(@Param("authorId") UUID authorId);
}
