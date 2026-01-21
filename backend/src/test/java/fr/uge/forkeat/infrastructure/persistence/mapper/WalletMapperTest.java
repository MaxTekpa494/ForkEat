package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.WalletMapper;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.model.UserStatus;
import fr.uge.forkeat.service.model.Wallet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class WalletMapperTest {

    private WalletMapper walletMapper = new WalletMapper();

    private UserEntity user(){
        var user = new UserEntity();
        user.setUsername("testuser");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@test.com");
        user.setPassword("hashedpassword123");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        return user;
    }

    @Test
    void entityToWallet(){
        var entity = new WalletEntity(1000L, user());

        var wallet = walletMapper.toDomain(entity);

        assert(entity.getUser().getId() == wallet.id());
        assert(Objects.equals(entity.getBalance(), wallet.balance()));

    }

    @Test
    void walletUpdate(){
        var wallet = new Wallet(UUID.randomUUID(), 2000L, user().getId(), Instant.now());
        var entity = new WalletEntity(1000L, user());

        walletMapper.updateEntity(entity, wallet);

        assert(Objects.equals(entity.getBalance(), wallet.balance()));
    }
}
