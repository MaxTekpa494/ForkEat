package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeSummaryView;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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
            SELECT r FROM RecipeEntity r
            LEFT JOIN r.ingredients ri
            LEFT JOIN ri.ingredient i
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
            GROUP BY r.id
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
    Page<RecipeEntity> searchRecipes(
            @Param("status") RecipeStatus status,
            @Param("search") String search,
            @Param("allergens") List<String> allergens,
            Pageable pageable
    );

    Page<RecipeSummaryView> findByAuthorUsernameAndStatus(String username, RecipeStatus status, Pageable pageable);
}
