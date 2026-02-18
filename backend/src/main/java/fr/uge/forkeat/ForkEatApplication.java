package fr.uge.forkeat;

import fr.uge.forkeat.infrastructure.config.StripeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

//Main Application
@SpringBootApplication
@EnableConfigurationProperties(StripeProperties.class)
public class ForkEatApplication {

	static void main(String[] args) {
		SpringApplication.run(ForkEatApplication.class, args);
	}
}
