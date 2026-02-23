package fr.uge.forkeat.service;

import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static reactor.core.publisher.Mono.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserPersistence userPersistence;

    @Mock
    private PasswordEncoder passwordEncoder;


    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userPersistence, passwordEncoder);
    }

    @Nested
    class likeUnlikeRecipe {
        @Test
        public void LikeShouldBeOk() {
            doNothing().when(userPersistence).likeRecipe(any(), any());
            userService.likeRecipe(UUID.randomUUID(), UUID.randomUUID());
        }

        @Test
        public void UnlikeShouldBeOk() {
            doNothing().when(userPersistence).unlikeRecipe(any(), any());
            userService.unlikeRecipe(UUID.randomUUID(), UUID.randomUUID());
        }
    }

}
