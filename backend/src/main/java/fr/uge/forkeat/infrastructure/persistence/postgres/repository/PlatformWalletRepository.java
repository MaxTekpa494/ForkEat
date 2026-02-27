package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.PlatformWalletEntity;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformWalletRepository extends JpaRepository<PlatformWalletEntity, UUID> {
    Optional<PlatformWalletEntity> findByType(PlatformWalletType type);
}
