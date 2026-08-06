package fr.uge.forkeat.infrastructure.config;

import com.google.api.client.util.ExponentialBackOff;
import fr.uge.forkeat.infrastructure.exception.DuplicateRecipeException;
import fr.uge.forkeat.infrastructure.exception.RecipeDeserializationException;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
@RequiredArgsConstructor
@EnableKafka
class KafkaConsumerConfig {

  private static final int THREAD_PARTITION = 3; // Pas le bon endroit

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory){
    return new KafkaTemplate<>(producerFactory);
  }





  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> recipeKafkaListenerContainerFactory
          (
                  ConsumerFactory<String, String> consumerFactory,
                  KafkaTemplate<String, String> kafkaTemplate
          ){
    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory);
    // 3 threads = 3 partitions du topic recipes.raw
    factory.setConcurrency(THREAD_PARTITION);
    factory.getContainerProperties().setAckMode(
            ContainerProperties.AckMode.MANUAL // Le listener acquitte lui-même via Acknowledgment
    );

    var recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> new TopicPartition(
                    record.topic() + ".DLT", // Dead Letter Topic
                    record.partition()
            )
    );
    var backOff = new ExponentialBackOffWithMaxRetries(5);
    backOff.setInitialInterval(1_000L);
    backOff.setMultiplier(2.0);
    backOff.setMaxInterval(30_000L);

    var errorHandler = new DefaultErrorHandler(recoverer, backOff);
    errorHandler.addNotRetryableExceptions(
            RecipeDeserializationException.class,
            DuplicateRecipeException.class
    );
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }
}
