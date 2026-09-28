package fr.uge.forkeat.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bucket4j.rate-limit")
public record RateLimitProperties(int capacity, int refillPeriodMinutes, List<String> paths) {}
