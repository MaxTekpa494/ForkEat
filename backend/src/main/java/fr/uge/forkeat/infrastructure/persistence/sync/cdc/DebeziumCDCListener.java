package fr.uge.forkeat.infrastructure.persistence.sync.cdc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class DebeziumCDCListener {

    private static final Logger log = LoggerFactory.getLogger(DebeziumCDCListener.class);

    private final Neo4jSyncService neo4jSyncService;
    private final ObjectMapper objectMapper;

    public DebeziumCDCListener(Neo4jSyncService neo4jSyncService, ObjectMapper objectMapper) {
        this.neo4jSyncService = neo4jSyncService;
        this.objectMapper = objectMapper;
    }

    public void handleChangeEvent(RecordChangeEvent<SourceRecord> record) {
        var sourceRecord = record.record();
        if (sourceRecord.value() == null) {
            return;
        }
        try {
            var value = (Struct) sourceRecord.value();
            var operation = value.getString("op");
            var source = value.getStruct("source");
            var table = source.getString("table");
            var payload = convertStructToJson(value);
            log.debug("### Événement reçu - Table: {}, Operation: {} ###", table, operation);
            switch (table) {
                case "users" -> neo4jSyncService.handleUserChange(operation, payload);
                case "recipes" -> neo4jSyncService.handleRecipeChange(operation, payload);
                default -> log.debug("Ignoring change for table: {}", table);
            }
        } catch (Exception e) {
            log.error("### Erreur lors du traitement de l'événement: {} ###", e.getMessage(), e);
        }
    }

    private JsonNode convertStructToJson(Struct struct) {
        try {
            return objectMapper.valueToTree(convertValue(struct));
        } catch (Exception e) {
            log.error("Error converting Struct to JSON: {}", e.getMessage(), e);
            return objectMapper.createObjectNode();
        }
    }

    private Object convertValue(Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case Struct s -> {
                var map = new HashMap<String, Object>();
                for (var field : s.schema().fields()) {
                    map.put(field.name(), convertValue(s.get(field)));
                }
                return map;
            }
            case List<?> list -> {
                return list.stream().map(this::convertValue).toList();
            }
            default -> {}
        }
        return value;
    }

}
