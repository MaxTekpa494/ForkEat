package fr.uge.forkeat.infrastructure.runner;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.User;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CreationAdminRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;



    public CreationAdminRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        var admin = new User(adminUsername, passwordEncoder.encode(adminPassword), "admin@admin.com", "ADMIN");
        userRepository.save(admin);
    }
}
