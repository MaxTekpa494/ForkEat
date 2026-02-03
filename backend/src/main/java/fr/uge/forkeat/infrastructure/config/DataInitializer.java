package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * This file is temporary will be deleted for the merge with develop
 */

@Configuration
public class DataInitializer {


    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;


    @Bean
    public CommandLineRunner initData(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        return args -> {

            if (userRepo.count() == 1) {
                var admin = new UserEntity(adminUsername, "admin", "admin", passwordEncoder.encode(adminPassword), "admin@admin.com", UserRole.ADMIN, UserStatus.ACTIVE, AuthMode.LOCAL);
                userRepo.save(admin);
            }
        };
    }
}