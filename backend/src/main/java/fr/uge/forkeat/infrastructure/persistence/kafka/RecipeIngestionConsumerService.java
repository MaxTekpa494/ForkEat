package fr.uge.forkeat.infrastructure.persistence.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.exception.RecipeDeserializationException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
class RecipeIngestionConsumerService<K, V> {

  private final ObjectMapper mapper;
  private final Logger logger = LoggerFactory.getLogger(RecipeIngestionConsumerService.class);

  public RecipeIngestionConsumerService(ObjectMapper mapper){
    this.mapper = mapper;
  }


  // ackMode = "MANUAL" version 4.1.x à la place de containerFactory = "kafkaManualAckListenerContainerFactory
  @KafkaListener(topics = "${spring.kafka.producer.properties.topic}", groupId = "${spring.kafka.consumer.group-id:recipe-ingestion-group}", containerFactory = "kafkaManualAckListenerContainerFactory")
  public void consume(ConsumerRecord<K, V> record, Acknowledgment ack){
    var key = record.key();
    logger.info("Message reçu => key={} partition={} offset={}", key, record.partition(), record.offset());

    RecipeRawEvent recipeRawEvent;
    try{
      recipeRawEvent = mapper.readValue(record.toString(), RecipeRawEvent.class);
      ack.acknowledge();
    } catch (JsonProcessingException e) {
      logger.info("JSON invalide pour key={}: {}", key, e.getMessage());
      throw new RecipeDeserializationException("JSON invalide : "+e.getMessage());
    }


  }


}
