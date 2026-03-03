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
        // Boucle de retry sans politique externe : on reste bloqué sur le même event jusqu'au succès.
        // Tant qu'on ne retourne pas, Debezium ne flush pas l'offset de ce record → PostgreSQL
        // ne considère pas l'event comme consommé → il sera re-livré au redémarrage si l'appli crash.
        // Cela garantit une sémantique at-least-once sans configuration supplémentaire.
        while (true) {
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
                    case "super_likes" -> neo4jSyncService.handleSuperLikeChange(operation, payload);
                    default -> log.debug("Ignoring change for table: {}", table);
                }
                return; // succès : on sort de la boucle, l'offset peut avancer
            } catch (Exception e) {
                // Si le thread est interrompu (arrêt de l'application via engine.close()),
                // on propage l'exception vers Debezium sans retourner normalement.
                // Un return ici ferait croire à Debezium que le record a été traité → offset avancé à tort.
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Arrêt du moteur Debezium", e);
                }
                log.error("### Erreur lors du traitement de l'événement, retry dans 5s: {} ###", e.getMessage(), e);
                try {
                    //noinspection BusyWait
                    Thread.sleep(5_000); // pause volontaire avant retry, pas du busy-waiting
                } catch (InterruptedException ie) {
                    // Même logique : interruption pendant le sleep = arrêt demandé,
                    // on propage pour ne pas avancer l'offset.
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Arrêt du moteur Debezium", ie);
                }
            }
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
        return switch (value) {
            case null -> null;
            case Struct s -> {
                var map = new HashMap<String, Object>();
                for (var field : s.schema().fields()) {
                    map.put(field.name(), convertValue(s.get(field)));
                }
                yield map;
            }
            case List<?> list -> list.stream().map(this::convertValue).toList();
            default -> value;
        };
    }

}
