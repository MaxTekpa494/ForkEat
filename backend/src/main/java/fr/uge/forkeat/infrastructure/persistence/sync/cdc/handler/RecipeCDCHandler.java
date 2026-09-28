package fr.uge.forkeat.infrastructure.persistence.sync.cdc.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.uge.forkeat.infrastructure.persistence.sync.cdc.RecipeNodeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Component
public class RecipeCDCHandler implements CDCTableHandler {

    private static final Logger logger = LoggerFactory.getLogger(RecipeCDCHandler.class);
    private final RecipeNodeClient recipeNodeClient;

    public RecipeCDCHandler(RecipeNodeClient recipeNodeClient) {
        this.recipeNodeClient = recipeNodeClient;
    }

    @Override
    public String tableName() {
        return "recipes";
    }

    @Override
    @Transactional
    public void handle(String operation, JsonNode payload) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(payload);
        switch (operation) {
            case "c", "r", "u" -> handleCreateOrUpdate(payload);
            case "d" -> handleDelete(payload);
            default -> logger.warn("Unknown operation: {}", operation);
        }
    }

    private void handleCreateOrUpdate(JsonNode payload) {
        //var before = payload.get("before");
        var op = payload.get("op").asText();
        var after = payload.get("after");
        if (after == null) return;
        var state = extractState(after);
        var id = extractId(after);
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

    private void handleDelete(JsonNode payload) {
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

}