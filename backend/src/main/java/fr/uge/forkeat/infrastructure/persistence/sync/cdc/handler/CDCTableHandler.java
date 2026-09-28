package fr.uge.forkeat.infrastructure.persistence.sync.cdc.handler;

import com.fasterxml.jackson.databind.JsonNode;

public interface CDCTableHandler {
    String tableName();
    void handle(String operation, JsonNode payload);
}