package fr.uge.forkeat.infrastructure.persistence.sync.cdc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class RecipeNodeClient {
    private final Neo4jClient neo4jClient;

    @Value("${app.system.earnings.username}")
    private String systemEarningsUsername;

    public RecipeNodeClient(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public void mergeRecipe(String id, String title, String authorId, String op) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        Objects.requireNonNull(op);
        var cypher = """
            MERGE (r:Recipe {id: $id})
            ON CREATE SET r.title = $title
            ON MATCH SET r.title = $title
            """;
        neo4jClient.query(cypher)
            .bind(id).to("id")
            .bind(title).to("title")
            .run();
        if (authorId != null && (op.equals("c") || op.equals("r"))) {
            createPublishedRelationship(id, authorId);
        }
    }

    private void createPublishedRelationship(String recipeId, String authorId) {
        var cypher = """
            MATCH (recipe:Recipe {id: $recipeId})
            OPTIONAL MATCH (oldAuthor:User)-[rel:PUBLISHED]->(recipe)
            DELETE rel
            WITH recipe
            MATCH (newAuthor:User {id: $authorId})
            MERGE (newAuthor)-[rel:PUBLISHED]->(recipe)
            ON CREATE SET rel.date = datetime()
            """;
        neo4jClient.query(cypher)
            .bind(authorId).to("authorId")
            .bind(recipeId).to("recipeId")
            .run();
    }

    public void deleteSocialRelationships(String recipeId) {
        Objects.requireNonNull(recipeId);
        var cypher = """
            MATCH (r:Recipe {id: $recipeId})
            OPTIONAL MATCH (r)<-[rel:LIKED|FOLLOWS_RECIPE]-()
            DELETE rel
            """;
        neo4jClient.query(cypher)
            .bind(recipeId).to("recipeId")
            .run();
    }

    public void reassignToSystemEarnings(String recipeId) {
        Objects.requireNonNull(recipeId);
        var cypher = """
            MATCH (r:Recipe {id: $recipeId})
            MATCH (u:User {username: $systemUsername})
            OPTIONAL MATCH (:User)-[oldPub:PUBLISHED]->(r)
            DELETE oldPub
            MERGE (u)-[:PUBLISHED {date: datetime()}]->(r)
            """;
        neo4jClient.query(cypher)
            .bind(recipeId).to("recipeId")
            .bind(systemEarningsUsername).to("systemUsername")
            .run();
    }

    public void deleteRecipeNode(String recipeId) {
        Objects.requireNonNull(recipeId);
        var cypher = "MATCH (r:Recipe {id: $recipeId}) DETACH DELETE r";
        neo4jClient.query(cypher)
            .bind(recipeId).to("recipeId")
            .run();
    }

    public void reassignVariantParent(String deletedRecipeId) {
        Objects.requireNonNull(deletedRecipeId);
        var cypher = """
            MATCH (child:Recipe)-[oldRel:IS_VARIANT_OF]->(deleted:Recipe {id: $deletedRecipeId})
            OPTIONAL MATCH (deleted)-[:IS_VARIANT_OF]->(newParent:Recipe)
            DELETE oldRel
            WITH child, newParent
            WHERE newParent IS NOT NULL
            CREATE (child)-[:IS_VARIANT_OF]->(newParent)
            """;
        neo4jClient.query(cypher)
            .bind(deletedRecipeId).to("deletedRecipeId")
            .run();
    }

    public void createVariantRelationship(String recipeId, String parentId) {
        Objects.requireNonNull(recipeId);
        Objects.requireNonNull(parentId);
        var cypher = """
            MATCH (r:Recipe {id: $recipeId})
            MATCH (parent:Recipe {id: $parentId})
            MERGE (r)-[:IS_VARIANT_OF]->(parent)
            """;
        neo4jClient.query(cypher)
            .bind(recipeId).to("recipeId")
            .bind(parentId).to("parentId")
            .run();
    }

    public boolean hasSuperLikedRelationships(String recipeId) {
        var cypher = "MATCH ()-[r:SUPER_LIKED]->(recipe:Recipe {id: $recipeId}) RETURN COUNT(r) > 0 AS hasSuperLikes";
        return neo4jClient.query(cypher)
            .bind(recipeId).to("recipeId")
            .fetchAs(Boolean.class)
            .one()
            .orElse(false);
    }

    public void markDeletedAt(String recipeId, Instant deletedAt) {
        Objects.requireNonNull(recipeId);
        Objects.requireNonNull(deletedAt);
        var cypher = "MATCH (r:Recipe {id: $recipeId}) SET r.deleted_at = $deletedAt";
        neo4jClient.query(cypher)
            .bind(recipeId).to("recipeId")
            .bind(deletedAt.toString()).to("deletedAt")
            .run();
    }
}
