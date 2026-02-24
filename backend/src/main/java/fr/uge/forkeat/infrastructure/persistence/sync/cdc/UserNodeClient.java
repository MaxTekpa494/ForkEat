package fr.uge.forkeat.infrastructure.persistence.sync.cdc;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserNodeClient {

    private final Neo4jClient neo4jClient;

    public UserNodeClient(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public void mergeUser(String id, String username, String email) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(username);
        Objects.requireNonNull(email);
        var cypher = """
            MERGE (u:User {id: $id})
            ON CREATE SET u.username = $username, u.email = $email
            ON MATCH SET u.username = $username, u.email = $email
            """;
        neo4jClient.query(cypher)
            .bind(id).to("id")
            .bind(username).to("username")
            .bind(email).to("email")
            .run();
    }

    public void deleteAllUserRelationships(String userId) {
        Objects.requireNonNull(userId);
        var cypher = """
            MATCH (u:User {id: $userId})
            OPTIONAL MATCH (u)-[r1:FOLLOWS|FOLLOWS_RECIPE|LIKED|PUBLISHED]->()
            OPTIONAL MATCH (u)<-[r2:FOLLOWS]-()
            DELETE r1, r2
            """;
        neo4jClient.query(cypher)
            .bind(userId).to("userId")
            .run();
    }

    public boolean hasSuperLikedRelationships(String userId) {
        Objects.requireNonNull(userId);
        var cypher = "MATCH (u:User {id: $userId})-[r:SUPER_LIKED]->() RETURN COUNT(r) > 0 AS hasSuperLikes";
        return neo4jClient.query(cypher)
            .bind(userId).to("userId")
            .fetchAs(Boolean.class)
            .one()
            .orElse(false);
    }

    public void deleteUserNode(String userId) {
        Objects.requireNonNull(userId);
        var cypher = "MATCH (u:User {id: $userId}) DELETE u";
        neo4jClient.query(cypher)
            .bind(userId).to("userId")
            .run();
    }

    public void markAsDeleted(String userId) {
        Objects.requireNonNull(userId);
        var cypher = "MATCH (u:User {id: $userId}) SET u.deleted = true";
        neo4jClient.query(cypher)
            .bind(userId).to("userId")
            .run();
    }

    public void reassignRecipesToSystemEarnings(String userId) {
        Objects.requireNonNull(userId);
        var cypher = """
            MATCH (u:User {id: $userId})-[oldPub:PUBLISHED]->(r:Recipe)
            MATCH (system:User {username: 'system_earnings'})
            DELETE oldPub
            MERGE (system)-[:PUBLISHED {date: datetime()}]->(r)
            """;
        neo4jClient.query(cypher)
            .bind(userId).to("userId")
            .run();
    }

    public void addSuperLike(String userId, String recipeId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(recipeId);
        var cypher = """
            MATCH (u:User {id: $userId})
            MATCH (r:Recipe {id: $recipeId})
            MERGE (u)-[:SUPER_LIKED]->(r)
            """;
        neo4jClient.query(cypher)
                .bind(userId).to("userId")
                .bind(recipeId).to("recipeId")
                .run();
    }

}
