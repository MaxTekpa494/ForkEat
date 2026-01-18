package fr.uge.forkeat.infrastructure.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        Map<String, Object> dotenvProperties = new HashMap<>();
        dotenv.entries().forEach(entry -> dotenvProperties.put(entry.getKey(), entry.getValue()));

        applicationContext.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("dotenv", dotenvProperties));
    }
}
