package fr.uge.forkeat.infrastructure.persistence.sync.cdc.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.uge.forkeat.infrastructure.persistence.sync.cdc.UserNodeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class UserCDCHandler implements CDCTableHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserCDCHandler.class);
    private final UserNodeClient userNodeClient;

    public UserCDCHandler(UserNodeClient userNodeClient) {
        this.userNodeClient = userNodeClient;
    }

    @Override
    public String tableName() {
        return "users";
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

    private void handleDelete(JsonNode payload) {
        var before = payload.get("before");
        if (before == null) return;
        var id = extractId(before);
        if (id == null) {
            logger.warn("User ID is missing or null in delete payload");
            return;
        }
        userNodeClient.reassignRecipesToSystemEarnings(id);
        logger.info("Reassigned recipes of user {} to system_earnings.", id);
        if (userNodeClient.hasRecipeInteractionRelationships(id)) {
            // L'utilisateur a des interactions avec des recettes (LIKED, FOLLOWS_RECIPE, SUPER_LIKED)
            // On conserve ces relations pour ne pas altérer les compteurs des recettes
            userNodeClient.deleteFollowRelationships(id);
            userNodeClient.markAsDeleted(id);
            logger.info("User {} marked as deleted. Recipe interactions (likes/follows/superLikes) preserved.", id);
        } else {
            // Aucune interaction avec des recettes, suppression complète
            userNodeClient.deleteAllUserRelationships(id);
            userNodeClient.deleteUserNode(id);
            logger.info("Deleted user node: {}", id);
        }
    }

    private String extractId(JsonNode node) {
        if (node == null || !node.has("id")) return null;
        var idNode = node.get("id");
        if (idNode.isNull()) return null;
        return idNode.asText();
    }
}