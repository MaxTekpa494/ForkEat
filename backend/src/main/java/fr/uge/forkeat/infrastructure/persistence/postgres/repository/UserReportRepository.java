package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.service.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserReportRepository extends JpaRepository<UserReportEntity, UUID> {

    List<UserReportEntity> findByReportedUserId(UUID reportedUserId);

    List<UserReportEntity> findByStatus(ReportStatus status);

    List<UserReportEntity> findByReporterId(UUID reporterId);

    boolean existsByReportedUserIdAndReporterId(UUID reportedUserId, UUID reporterId);

    long countByStatus(ReportStatus status);
}