package fr.uge.forkeat.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Ajoute le support des types Java 8 Date/Time (LocalDate, LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        // Désactive l'écriture des dates sous forme de timestamps numériques
        // Les dates seront formatées en ISO-8601 (ex: "2026-01-19T10:30:00")
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
