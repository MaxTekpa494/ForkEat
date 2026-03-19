package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.RecipeNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.ChainNodeProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.EarningsRowProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RedistributionChainRowProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RedistributionSummaryProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.UnprocessedSLProjection;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface Neo4jRedistributionRepository extends Neo4jRepository<RecipeNode, UUID> {

    @Query("""
            MATCH (sender:User)-[sl:SUPER_LIKED]->(recipe:Recipe)
            WHERE sl.batch_month IS NULL
            RETURN sl.id                              AS superLikeId,
                   recipe.id                         AS recipeId,
                   coalesce(sl.redist_amount_cents, 0) AS redistAmount,
                   sl.date                           AS date
            """)
    List<UnprocessedSLProjection> findUnprocessedSuperLikes();

    @Query("""
            MATCH (r:Recipe {id: $recipeId})<-[:PUBLISHED]-(directAuthor:User)
            RETURN directAuthor.id AS authorId, r.id AS recipeId, 0 AS depth
            UNION ALL
            MATCH (root:Recipe {id: $recipeId})-[varPath:IS_VARIANT_OF*1..]->(ancestor:Recipe)<-[:PUBLISHED]-(ancestorAuthor:User)
            WHERE ancestor.deleted_at IS NULL OR ancestor.deleted_at > $atDate
            RETURN ancestorAuthor.id AS authorId, ancestor.id AS recipeId, size(varPath) AS depth
            """)
    List<ChainNodeProjection> getAuthorChain(@Param("recipeId") String recipeId,
                                             @Param("atDate") OffsetDateTime atDate);

    @Query("""
            MATCH (r:Recipe {id: $recipeId})-[:IS_VARIANT_OF*1..]->(ancestor:Recipe)
            WHERE ancestor.deleted_at IS NOT NULL
              AND ancestor.deleted_at >= $oldest
              AND ancestor.deleted_at <= $newest
            RETURN count(ancestor) > 0
            """)
    boolean hasChainChangedBetween(@Param("recipeId") String recipeId,
                                   @Param("oldest") OffsetDateTime oldest,
                                   @Param("newest") OffsetDateTime newest);

    @Query("""
            MATCH (u:User {id: $authorId})-[rr:REDISTRIBUTION_RECEIVED {
                batch_month: $batchMonth,
                superliked_recipe_id: $sourceRecipeId
            }]->(:Recipe)
            RETURN count(rr) > 0
            """)
    boolean existsRedistributionFor(@Param("authorId") String authorId,
                                    @Param("sourceRecipeId") String sourceRecipeId,
                                    @Param("batchMonth") String batchMonth);

    @Query("""
            MATCH (u:User {id: $authorId})
            MATCH (r:Recipe {id: $authorRecipeId})
            CREATE (u)-[:REDISTRIBUTION_RECEIVED {
                batch_month:          $batchMonth,
                superliked_recipe_id: $sourceRecipeId,
                amount_cents:         $amountCents
            }]->(r)
            """)
    void saveAuthorRedistribution(@Param("authorId") String authorId,
                                  @Param("authorRecipeId") String authorRecipeId,
                                  @Param("sourceRecipeId") String sourceRecipeId,
                                  @Param("amountCents") long amountCents,
                                  @Param("batchMonth") String batchMonth);

    @Query("""
            MATCH ()-[sl:SUPER_LIKED]->()
            WHERE sl.id IN $superLikeIds
            SET sl.batch_month = $batchMonth
            """)
    void markSuperLikesProcessed(@Param("superLikeIds") List<String> superLikeIds,
                                 @Param("batchMonth") String batchMonth);

    @Query("""
            MATCH (b:Recipe)
            WHERE b.deleted_at IS NOT NULL
            AND NOT EXISTS {
                MATCH ()-[sl:SUPER_LIKED]->(b)
                WHERE sl.batch_month IS NULL
            }
            AND NOT EXISTS {
                MATCH (child:Recipe)-[:IS_VARIANT_OF]->(b)
                MATCH ()-[sl:SUPER_LIKED]->(child)
                WHERE sl.batch_month IS NULL AND sl.date < b.deleted_at
            }
            WITH b
            OPTIONAL MATCH (child:Recipe)-[childRel:IS_VARIANT_OF]->(b)
            OPTIONAL MATCH (b)-[:IS_VARIANT_OF]->(parent:Recipe)
            DELETE childRel
            WITH b, child, parent
            WHERE child IS NOT NULL AND parent IS NOT NULL
            CREATE (child)-[:IS_VARIANT_OF]->(parent)
            WITH DISTINCT b
            OPTIONAL MATCH (b)-[rel:IS_VARIANT_OF]->()
            DELETE rel
            """)
    void cleanupAfterBatch();

    @Query("""
            MATCH ()-[r:REDISTRIBUTION_RECEIVED {batch_month: $batchMonth}]->()
            RETURN coalesce(sum(r.amount_cents), 0)
            """)
    long getTotalRedistributedForMonth(@Param("batchMonth") String batchMonth);

    @Query("""
            MATCH (u:User {id: $userId})-[r:REDISTRIBUTION_RECEIVED]->(recipe:Recipe)
            RETURN r.batch_month          AS batchMonth,
                   r.amount_cents         AS amountCents,
                   r.superliked_recipe_id AS sourceRecipeId,
                   recipe.id              AS recipeId,
                   recipe.title           AS recipeTitle
            ORDER BY r.batch_month DESC, r.amount_cents DESC
            """)
    List<EarningsRowProjection> findEarningsByUser(@Param("userId") String userId);

    @Query("""
            MATCH (u:User)-[r:REDISTRIBUTION_RECEIVED]->(authorRecipe:Recipe)
            OPTIONAL MATCH (sourceRecipe:Recipe {id: r.superliked_recipe_id})
            RETURN r.batch_month                                       AS batchMonth,
                   r.superliked_recipe_id                              AS sourceRecipeId,
                   coalesce(sourceRecipe.title, 'Recette supprimée')   AS sourceRecipeTitle,
                   sum(r.amount_cents)                                 AS totalCents
            ORDER BY r.batch_month DESC, totalCents DESC
            """)
    List<RedistributionSummaryProjection> findRedistributionSummary();

    @Query("""
            MATCH (u:User)-[r:REDISTRIBUTION_RECEIVED {
                superliked_recipe_id: $recipeId,
                batch_month: $batchMonth
            }]->(authorRecipe:Recipe)
            RETURN u.id            AS authorId,
                   u.username      AS username,
                   authorRecipe.id AS recipeId,
                   authorRecipe.title AS recipeTitle,
                   r.amount_cents  AS amountCents
            ORDER BY r.amount_cents DESC
            """)
    List<RedistributionChainRowProjection> findChainForRecipeAndMonth(
            @Param("recipeId") String recipeId,
            @Param("batchMonth") String batchMonth);
}