package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserJpaRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletJpaRepository;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.model.UserStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * This file is temporary will be deleted for the merge with develop
 */

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserJpaRepository userRepo, WalletJpaRepository walletRepo) {
        return args -> {
            if (userRepo.count() == 1) {
                var user = new UserEntity();
                user.setUsername("TestUser");
                user.setFirstName("Jean");
                user.setLastName("Testeur");
                user.setEmail("test@user.com");
                user.setPassword("password123"); // (En vrai il faudrait le hacher et faire un burger)
                user.setRole(UserRole.MEMBER);
                user.setStatus(UserStatus.ACTIVE);
                user.setAuthMode(AuthMode.LOCAL);
                user.setCreatedAt(Instant.now());
                user.setUpdatedAt(Instant.now());
                userRepo.save(user);

                WalletEntity wallet = new WalletEntity(0L, user);  // 0 centimes
                walletRepo.save(wallet);

                System.out.println("Données de test initialisées : User ID " + user.getId() + " créé avec un Wallet vide");
            }
        };
    }
}