package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserJpaRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserJpaRepository userRepo, WalletJpaRepository walletRepo) {
        return args -> {
            if (userRepo.count() == 0) {
                var user = new UserEntity();
                user.setEmail("test@user.com");
                user.setPassword("password123"); // (En vrai il faudrait le hacher et faire un burger)
                user.setUsername("TestUser");
                user.setFirstName("Jean");
                user.setLastName("Testeur");
                user.setAuthentificationMode(AuthMode.LOCAL);
                userRepo.save(user);

                WalletEntity wallet = new WalletEntity(user);
                wallet.setBalance(0L); // 0 centimes
                walletRepo.save(wallet);

                System.out.println("Données de test initialisées : User ID 1 créé avec un Wallet vide");
            }
        };
    }
}