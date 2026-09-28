package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.PromotionEntity;
import fr.uge.forkeat.service.model.superlike.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<PromotionEntity, UUID> {

    @Query("""
            SELECT p FROM PromotionEntity p
            WHERE p.status = fr.uge.forkeat.service.model.superlike.PromotionStatus.ACTIVE
            AND p.startsAt <= :instant
            AND (p.endsAt IS NULL OR p.endsAt > :instant)
            """)
    Optional<PromotionEntity> findActiveAt(@Param("instant") Instant instant);

    @Query("""
            SELECT p FROM PromotionEntity p
            WHERE p.status IN (
                fr.uge.forkeat.service.model.superlike.PromotionStatus.SCHEDULED,
                fr.uge.forkeat.service.model.superlike.PromotionStatus.ACTIVE
            )
            ORDER BY p.startsAt ASC
            """)
    List<PromotionEntity> findScheduledOrActive();

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) > 0 FROM promotions p
            WHERE p.status::text NOT IN ('CANCELLED', 'EXPIRED')
            AND p.starts_at < COALESCE(CAST(:endsAt AS timestamptz), 'infinity'::timestamptz)
            AND (p.ends_at IS NULL OR p.ends_at > CAST(:startsAt AS timestamptz))
            """)
    boolean hasOverlappingAny(@Param("startsAt") Instant startsAt,
                              @Param("endsAt") Instant endsAt);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) > 0 FROM promotions p
            WHERE p.id != CAST(:excludeId AS uuid)
            AND p.status::text NOT IN ('CANCELLED', 'EXPIRED')
            AND p.starts_at < COALESCE(CAST(:endsAt AS timestamptz), 'infinity'::timestamptz)
            AND (p.ends_at IS NULL OR p.ends_at > CAST(:startsAt AS timestamptz))
            """)
    boolean hasOverlappingExcluding(@Param("startsAt") Instant startsAt,
                                    @Param("endsAt") Instant endsAt,
                                    @Param("excludeId") UUID excludeId);

    @Modifying
    @Query("UPDATE PromotionEntity p SET p.status = :newStatus WHERE p.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("newStatus") PromotionStatus newStatus);

    @Query("""
            SELECT p FROM PromotionEntity p
            WHERE p.status = fr.uge.forkeat.service.model.superlike.PromotionStatus.SCHEDULED
            AND p.startsAt <= :now
            """)
    List<PromotionEntity> findScheduledToActivate(@Param("now") Instant now);

    @Query("""
            SELECT p FROM PromotionEntity p
            WHERE p.status = fr.uge.forkeat.service.model.superlike.PromotionStatus.ACTIVE
            AND p.endsAt IS NOT NULL
            AND p.endsAt <= :now
            """)
    List<PromotionEntity> findActiveToExpire(@Param("now") Instant now);
}
