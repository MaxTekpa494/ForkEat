package fr.uge.forkeat.infrastructure.persistence.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

@FunctionalInterface
public interface KafkaConsumer<K, V> {
  void consume(ConsumerRecord<K, V> record, Acknowledgment ack);
}
