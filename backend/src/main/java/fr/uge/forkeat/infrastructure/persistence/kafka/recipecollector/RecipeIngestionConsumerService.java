package fr.uge.forkeat.infrastructure.persistence.kafka.recipecollector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.exception.DuplicateRecipeException;
import fr.uge.forkeat.infrastructure.exception.RecipeDeserializationException;
import fr.uge.forkeat.infrastructure.persistence.kafka.KafkaConsumer;
import fr.uge.forkeat.infrastructure.persistence.kafka.recipecollector.RecipeIngestionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
class RecipeIngestionConsumerService<K, V> implements KafkaConsumer<K, V> {
  private final Logger logger = LoggerFactory.getLogger(RecipeIngestionConsumerService.class);
  private final ObjectMapper mapper;
  private final RecipeIngestionService recipeIngestionService;


  public RecipeIngestionConsumerService(ObjectMapper mapper, RecipeIngestionService recipeIngestionService){
    this.mapper = mapper;
    this.recipeIngestionService = recipeIngestionService;
  }


  // ackMode = "MANUAL" version 4.1.x à la place de containerFactory = "kafkaManualAckListenerContainerFactory
  // Attention : ConsumerRecord est faite pour recevoir les messages en batch et pour ça il faut mettre à jour
  // le config de recipeKafkaListenerContainerFactory avec factory.setBatchListener(true); et faire une boucle for ici
  @Override
  @KafkaListener(topics = "${spring.kafka.producer.properties.topic}", groupId = "${spring.kafka.consumer.group-id:recipe-ingestion-group}", containerFactory = "recipeKafkaListenerContainerFactory")
  public void consume(ConsumerRecord<K, V> record, Acknowledgment ack){
    var key = record.key();
    logger.info("Message reçu => key={} partition={} offset={}", key, record.partition(), record.offset());

    RecipeRawEvent recipeRawEvent;
    try{
      recipeRawEvent = mapper.readValue(record.value().toString(), RecipeRawEvent.class);
    } catch (JsonProcessingException e) {
      logger.info("JSON invalide pour key={}: {}", key, e.getMessage());
      throw new RecipeDeserializationException("JSON invalide : "+e.getMessage());
    }

    try{
      var recipe = recipeIngestionService.ingest(recipeRawEvent);
      logger.info("Nouvelle recette depuis Kafka avec la key: {}\n{}", key, recipe);
    } catch (IllegalArgumentException e) {
      throw new DuplicateRecipeException(e.getMessage());
    } catch (RuntimeException e) {
      logger.error("Erreur lors de l'ingestion de la recette key={}", key, e);
      throw e; // AVOIR UNE ERREUR PLUS COHERENTE ICI
    }
    ack.acknowledge();
  }
}
