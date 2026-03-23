package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.UserReportEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserReportRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.projection.UserReportDetails;
import fr.uge.forkeat.service.persistence.UserReportPersistence;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public final class UserReportPersistenceAdapter implements UserReportPersistence {

    private final UserReportRepository userReportRepository;
    private final UserRepository userRepository;

    public UserReportPersistenceAdapter(UserReportRepository userReportRepository,
                                        UserRepository userRepository) {
        this.userReportRepository = userReportRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserReport save(UserReport report) {
        Objects.requireNonNull(report);

        var reportedUser = userRepository.getReferenceById(report.reportedUserId());
        var reporter = report.reporterId() != null
                ? userRepository.getReferenceById(report.reporterId())
                : null;
        var reviewer = report.reviewedById() != null
                ? userRepository.getReferenceById(report.reviewedById())
                : null;

        var entity = UserReportEntityMapper.toEntity(report, reportedUser, reporter, reviewer);
        var saved = userReportRepository.save(entity);
        return UserReportEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<UserReport> findById(UUID userReportId) {
        Objects.requireNonNull(userReportId);
        return userReportRepository.findById(userReportId)
                .map(UserReportEntityMapper::toDomain);
    }

    @Override
    public List<UserReport> findByReportedUserId(UUID reportedUserId) {
        Objects.requireNonNull(reportedUserId);
        return userReportRepository.findByReportedUserId(reportedUserId).stream()
                .map(UserReportEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<UserReport> findByStatus(ReportStatus status) {
        Objects.requireNonNull(status);
        return userReportRepository.findByStatus(status).stream()
                .map(UserReportEntityMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<UserReportDetails> getReportsToModerateWithReportedUserAndReporter(UUID reporterId, int size, int page) {
        Objects.requireNonNull(reporterId);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var pageResult = userReportRepository.findUserReportsByStatusAndNotReporterIdWithReportedUserAndReporter(ReportStatus.PENDING, reporterId, pageable);
        var details = pageResult.getContent().stream()
                .map(UserReportEntityMapper::toDomain)
                .toList();
        return new PageResult<>(details, pageResult.getTotalElements());
    }

    @Override
    public boolean existsById(UUID userReportId) {
        Objects.requireNonNull(userReportId);
        return userReportRepository.existsById(userReportId);
    }

    @Override
    public boolean isAuthor(UUID userReportId, UUID reporterId) {
        Objects.requireNonNull(userReportId);
        Objects.requireNonNull(reporterId);
        return userReportRepository.existsByIdAndReporterId(userReportId, reporterId);
    }

    @Override
    public boolean existsByReportedUserIdAndReporterId(UUID reportedUserId, UUID reporterId) {
        Objects.requireNonNull(reportedUserId);
        Objects.requireNonNull(reporterId);
        return userReportRepository.existsByReportedUserIdAndReporterId(reportedUserId, reporterId);
    }

}