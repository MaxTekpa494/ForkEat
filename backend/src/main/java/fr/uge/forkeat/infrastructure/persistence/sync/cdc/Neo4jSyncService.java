package fr.uge.forkeat.infrastructure.persistence.sync.cdc;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class Neo4jSyncService {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jSyncService.class);
    private final UserNodeClient userNodeClient;
    private final RecipeNodeClient recipeNodeClient;

    public Neo4jSyncService(UserNodeClient userNodeClient, RecipeNodeClient recipeNodeClient) {
        this.userNodeClient = userNodeClient;
        this.recipeNodeClient = recipeNodeClient;
    }

    @Transactional
    public void handleUserChange(String operation, JsonNode payload) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(payload);
        switch (operation) {
            case "c", "r", "u" -> handleUserCreateOrUpdate(payload);
            case "d" -> handleUserDelete(payload);
            default -> logger.warn("Unknown operation: {}", operation);
        }
    }

    @Transactional
    public void handleRecipeChange(String operation, JsonNode payload) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(payload);
        switch (operation) {
            case "c", "r", "u" -> handleRecipeCreateOrUpdate(payload);
            case "d" -> handleRecipeDelete(payload);
            default -> logger.warn("Unknown operation: {}", operation);
        }
    }

    @Transactional
    public void handleSuperLikeChange(String operation, JsonNode payload) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(payload);
        switch (operation) {
            case "c", "r" -> handleSuperLikeCreate(payload);
            case "d", "u" -> logger.warn("Impossible operation: {}", operation);
            default -> logger.warn("Unknown operation: {}", operation);
        }
    }

    private String extractId(JsonNode node) {
        if (node == null || !node.has("id")) return null;
        var idNode = node.get("id");
        if (idNode.isNull()) return null;
        return idNode.asText();
    }

    private String extractState(JsonNode node) {
        if (node == null || !node.has("status")) return null;
        var stateNode = node.get("status");
        if (stateNode.isNull()) return null;
        return stateNode.asText();
    }

    private void handleUserCreateOrUpdate(JsonNode payload) {
        var after = payload.get("after");
        if (after == null) return;
        var id = extractId(after);
        if (id == null) {
            logger.warn("User ID is missing or null in payload");
            return;
        }
        var username = after.has("username") ? after.get("username").asText() : null;
        var email = after.has("email") ? after.get("email").asText() : null;
        userNodeClient.mergeUser(id, username, email);
        logger.info("Created/Updated user node: {}", id);
    }

    private void handleUserDelete(JsonNode payload) {
        var before = payload.get("before");
        if (before == null) return;
        var id = extractId(before);
        if (id == null) {
            logger.warn("User ID is missing or null in delete payload");
            return;
        }
        userNodeClient.reassignRecipesToSystemEarnings(id);
        logger.info("Reassigned recipes of user {} to system_earnings.", id);
        userNodeClient.deleteAllUserRelationships(id);
        if (userNodeClient.hasSuperLikedRelationships(id)) {
            // Le noeud User est conservé et marqué comme supprimé pour le batch mensuel
            userNodeClient.markAsDeleted(id);
            logger.info("User {} marked as deleted. Node kept for SUPER_LIKED financial records.", id);
        } else {
            // Pas de SUPER_LIKED, suppression complète du nœud
            userNodeClient.deleteUserNode(id);
            logger.info("Deleted user node: {}", id);
        }
    }

    private void handleRecipeCreateOrUpdate(JsonNode payload) {
        //var before = payload.get("before");
        var op = payload.get("op").asText();
        var after = payload.get("after");
        if (after == null) return;
        var id = extractId(after);
        var state = extractState(after);
        if (id == null) {
            logger.warn("Recipe ID is missing or null in payload");
            return;
        }
        var title = after.has("title") ? after.get("title").asText() : null;
        var authorId = after.has("author_id") && !after.get("author_id").isNull()
                ? after.get("author_id").asText() : null;
        recipeNodeClient.mergeRecipe(id, title, authorId, op, state);
        // Gestion simplifiée : Création de la relation uniquement si elle n'existe pas (cas CREATE)
        // Pour un UPDATE, on sait que la structure ne change pas sauf suppression gérée ailleurs.
        var newParentId = after.has("parent_id") && !after.get("parent_id").isNull() ? after.get("parent_id").asText() : null;
        if (newParentId != null && (op.equals("c") || op.equals("r"))) {
             recipeNodeClient.createVariantRelationship(id, newParentId);
        }
        logger.info("Created/Updated recipe node: {}", id);
    }

    private void handleRecipeDelete(JsonNode payload) {
        var before = payload.get("before");
        if (before == null) return;
        var id = extractId(before);
        if (id == null) {
            logger.warn("Recipe ID is missing or null in delete payload");
            return;
        }
        var hasSuperLikes = recipeNodeClient.hasSuperLikedRelationships(id);
        if (hasSuperLikes) {
            recipeNodeClient.deleteSocialRelationships(id);
            recipeNodeClient.reassignToSystemEarnings(id);
            recipeNodeClient.markDeletedAt(id, Instant.now());
            logger.info("Recipe {} has SUPER_LIKED. Reassigned to system_earnings and marked as deleted.", id);
        } else {
            recipeNodeClient.reassignVariantParent(id);
            recipeNodeClient.deleteRecipeNode(id);
            logger.info("Deleted recipe node {}. Variant hierarchy repaired.", id);
        }
    }

    private void handleSuperLikeCreate(JsonNode payload) {
        //var before = payload.get("before");
        var after = payload.get("after");
        if (after == null) return;
        var id = extractId(after);
        if (id == null) {
            logger.warn("Recipe ID is missing or null in payload");
            return;
        }
        var userId = after.has("user_id") && !after.get("user_id").isNull()
                ? after.get("user_id").asText() : null;
        var recipeId = after.has("recipe_id") && !after.get("recipe_id").isNull()
                ? after.get("recipe_id").asText() : null;

        var amount = after.has("amount") ? after.get("amount").asLong() : 0;
        var redistAmountCents = after.has("redist_amount_cents") ? after.get("redist_amount_cents").asLong() : 0;
        userNodeClient.addSuperLike(id, userId, recipeId, amount, redistAmountCents);

        logger.info("Created SuperLikeRelationShip: {}", id);
    }
}
