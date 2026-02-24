package fr.uge.forkeat.service;

import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.port.PasswordHasher;
import fr.uge.forkeat.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserPersistence userPersistence;

    @Mock
    private PasswordHasher passwordHasher;


    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userPersistence, passwordHasher);
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
