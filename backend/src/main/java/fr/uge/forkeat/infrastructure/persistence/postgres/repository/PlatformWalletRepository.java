package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.PlatformWalletEntity;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformWalletRepository extends JpaRepository<PlatformWalletEntity, UUID> {
    Optional<PlatformWalletEntity> findByType(PlatformWalletType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pw FROM PlatformWalletEntity pw WHERE pw.type = :type")
    Optional<PlatformWalletEntity> findByTypeWithLock(PlatformWalletType type);
}
