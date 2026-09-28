package fr.uge.forkeat.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class R2Config {

  @Value("${cloudflare.r2.access.key.id}")
  private String accessKeyId;

  @Value("${cloudflare.r2.secret.access.key}")
  private String secretAccessKey;

  @Value("${cloudflare.r2.endpoint}")
  private String endpoint;

  @Bean
  public S3Client s3Client() {
    var credential = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credential))
            .region(Region.of("auto"))
            .build();
  }
}
