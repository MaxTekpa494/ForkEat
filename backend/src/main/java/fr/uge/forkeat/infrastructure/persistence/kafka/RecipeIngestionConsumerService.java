package fr.uge.forkeat.infrastructure.persistence.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
class RecipeIngestionConsumerService<K, V> {

  private final ObjectMapper mapper;

  public RecipeIngestionConsumerService(ObjectMapper mapper){
    this.mapper = mapper;
  }


  // ackMode = "MANUAL" version 4.1.x à la place de containerFactory = "kafkaManualAckListenerContainerFactory
  @KafkaListener(topics = "${spring.kafka.producer.properties.topic}", containerFactory = "kafkaManualAckListenerContainerFactory")
  public void listen(ConsumerRecord<K, V> data, Acknowledgment ack){

    ack.acknowledge();
  }


}
