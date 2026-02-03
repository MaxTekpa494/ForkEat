package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletJpaRepository extends CrudRepository<WalletEntity, UUID> {

    /* * PESSIMISTIC_WRITE :
     * Dès qu'on cherche un wallet par userId, on pose un verrou.
     * Si deux paiements arrivent en même temps, le deuxième attendra ici que le premier finisse.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletEntity> findByUserId(@Param("userId") UUID userId);

    WalletEntity getReferenceById(UUID uuid);

    @Query("SELECT w.balance FROM WalletEntity w WHERE w.user.id = :userId")
    Long findBalanceByUserId(@Param("userId") UUID userId);
}