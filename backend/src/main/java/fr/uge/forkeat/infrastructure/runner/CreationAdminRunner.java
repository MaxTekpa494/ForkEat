package fr.uge.forkeat.infrastructure.runner;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.model.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CreationAdminRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EntityManager entityManager;


    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;




    public CreationAdminRunner(UserRepository userRepository, PasswordEncoder passwordEncoder, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    //Creating the first Admin.
    @Override
    public void run(String... args) throws Exception {
        var admin = new UserEntity(adminUsername, "admin", "admin", passwordEncoder.encode(adminPassword), "admin@admin.com", UserRole.ADMIN, UserStatus.ACTIVE, AuthMode.LOCAL);
       userRepository.save(admin);
    }
}
