package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

    /* * PESSIMISTIC_WRITE :
     * Dès qu'on cherche un wallet par userId, on pose un verrou.
     * Si deux paiements arrivent en même temps, le deuxième attendra ici que le premier finisse.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletEntity> findByUserId(@Param("userId") UUID userId);

    WalletEntity getReferenceById(UUID uuid);

    // On va plus en avoir besion quand ça va être du OneToOne
    @Query("SELECT w.balance FROM WalletEntity w WHERE w.user.id = :userId")
    Long findBalanceByUserIdReadOnly(@Param("userId") UUID userId);


    @Query("SELECT w.balance FROM WalletEntity w WHERE w.user.id = :userId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Long findBalanceByUserId(@Param("userId") UUID userId);


    @Query("SELECT w FROM WalletEntity w WHERE w.user.id = :userId")
    Optional<WalletEntity> findByUserIdReadOnly(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE WalletEntity w SET w.balance = w.balance - :amount WHERE w.user.id = :userId")
    void decrementBalanceByUserId(@Param("userId") UUID userId, @Param("amount") Long amount);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE WalletEntity w SET w.balance = w.balance + :amount WHERE w.id = :id")
    void incrementBalanceById(@Param("id") UUID id, @Param("amount") Long amount);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE WalletEntity w SET w.balance = w.balance - :amount WHERE w.id = :id")
    void decrementBalanceById(@Param("id") UUID id, @Param("amount") Long amount);

}