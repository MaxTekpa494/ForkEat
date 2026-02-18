package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.BankInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankInfoRepository extends JpaRepository<BankInfoEntity, UUID> {
    Optional<BankInfoEntity> findByUserId(UUID userId);
}
