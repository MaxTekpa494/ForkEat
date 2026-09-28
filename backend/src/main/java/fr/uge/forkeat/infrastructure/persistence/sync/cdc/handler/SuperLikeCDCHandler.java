package fr.uge.forkeat.infrastructure.persistence.sync.cdc.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.uge.forkeat.infrastructure.persistence.sync.cdc.UserNodeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class SuperLikeCDCHandler implements CDCTableHandler {

    private static final Logger logger = LoggerFactory.getLogger(SuperLikeCDCHandler.class);
    private final UserNodeClient userNodeClient;

    public SuperLikeCDCHandler(UserNodeClient userNodeClient) {
        this.userNodeClient = userNodeClient;
    }

    @Override
    public String tableName() {
        return "super_likes";
    }

    @Override
    @Transactional
    public void handle(String operation, JsonNode payload) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(payload);
        switch (operation) {
            case "c", "r" -> handleCreate(payload);
            case "d", "u" -> logger.warn("Impossible operation: {}", operation);
            default -> logger.warn("Unknown operation: {}", operation);
        }
    }

    private void handleCreate(JsonNode payload) {
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

    private String extractId(JsonNode node) {
        if (node == null || !node.has("id")) return null;
        var idNode = node.get("id");
        if (idNode.isNull()) return null;
        return idNode.asText();
    }
}