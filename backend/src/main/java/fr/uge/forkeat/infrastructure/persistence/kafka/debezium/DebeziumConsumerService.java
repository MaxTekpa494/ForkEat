package fr.uge.forkeat.infrastructure.persistence.kafka.debezium;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.persistence.kafka.KafkaConsumer;
import fr.uge.forkeat.infrastructure.persistence.sync.cdc.handler.CDCTableHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class DebeziumConsumerService<K, V> implements KafkaConsumer<K, V> {

  private final Logger logger = LoggerFactory.getLogger(DebeziumConsumerService.class);
  private static final String FORKEAT_PUBLIC = "forkeat.public\\.(.*)";
  private static final String groupID = "forkeat.public.debezium";


  private final ObjectMapper objectMapper;
  private final Map<String, CDCTableHandler> cdcTableHandlers;

  public DebeziumConsumerService(ObjectMapper objectMapper, List<CDCTableHandler> cdcTableHandlers) {
    this.objectMapper = objectMapper;
    this.cdcTableHandlers = cdcTableHandlers.stream()
        .collect(Collectors.toMap(CDCTableHandler::tableName, Function.identity()));
  }


  private boolean handleChangeEvent(ConsumerRecord<String, String> record) {
    try {
      JsonNode payload = objectMapper.readTree(record.value());
      var cdcTableHandler = cdcTableHandlers.get(payload.get("source").get("table").asText());
      if (cdcTableHandler != null) {
        var op = payload.get("op").asText();
        cdcTableHandler.handle(op, payload);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return true;
  }

  @Override
  @KafkaListener(topicPattern = FORKEAT_PUBLIC, groupId = groupID, containerFactory = "debeziumKafkaListenerContainerFactory")
  public void consume(ConsumerRecord<K, V> record, Acknowledgment ack) {
    var key = record.key();
    logger.info("Received key: {}", key);

    if(handleChangeEvent((ConsumerRecord<String, String>) record)) {
      ack.acknowledge();
    }
  }
}
