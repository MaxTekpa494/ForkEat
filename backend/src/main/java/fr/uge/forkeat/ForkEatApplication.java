package fr.uge.forkeat;

import fr.uge.forkeat.infrastructure.config.RateLimitProperties;
import fr.uge.forkeat.infrastructure.config.StripeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;

//Main Application
@SpringBootApplication
@EnableConfigurationProperties({StripeProperties.class, RateLimitProperties.class})
@org.springframework.scheduling.annotation.EnableScheduling
@EnableKafka
public class ForkEatApplication {
	static void main(String[] args) {
		SpringApplication.run(ForkEatApplication.class, args);
	}
}
