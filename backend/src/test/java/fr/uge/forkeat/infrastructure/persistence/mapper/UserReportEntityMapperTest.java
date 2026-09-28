package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.UserReportDetailsView;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.UserReportType;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserReportEntityMapperTest {

    private UserEntity reportedUserEntity;
    private UserEntity reporterEntity;
    private UserEntity reviewerEntity;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        reportedUserEntity = new UserEntity();
        reportedUserEntity.setId(UUID.randomUUID());
        reportedUserEntity.setUsername("target");
        reportedUserEntity.setFirstName("Alice");
        reportedUserEntity.setLastName("Martin");
        reportedUserEntity.setEmail("target@test.com");
        reportedUserEntity.setPassword("hashed");
        reportedUserEntity.setRole(UserRole.MEMBER);
        reportedUserEntity.setStatus(UserStatus.ACTIVE);
        reportedUserEntity.setAuthMode(AuthMode.LOCAL);

        reporterEntity = new UserEntity();
        reporterEntity.setId(UUID.randomUUID());
        reporterEntity.setUsername("reporter");
        reporterEntity.setFirstName("Jean");
        reporterEntity.setLastName("Dupont");
        reporterEntity.setEmail("reporter@test.com");
        reporterEntity.setPassword("hashed");
        reporterEntity.setRole(UserRole.MEMBER);
        reporterEntity.setStatus(UserStatus.ACTIVE);
        reporterEntity.setAuthMode(AuthMode.LOCAL);

        reviewerEntity = new UserEntity();
        reviewerEntity.setId(UUID.randomUUID());
        reviewerEntity.setUsername("moderator");
        reviewerEntity.setFirstName("Mod");
        reviewerEntity.setLastName("Era");
        reviewerEntity.setEmail("mod@test.com");
        reviewerEntity.setPassword("hashed");
        reviewerEntity.setRole(UserRole.MODERATOR);
        reviewerEntity.setStatus(UserStatus.ACTIVE);
        reviewerEntity.setAuthMode(AuthMode.LOCAL);
    }

    private UserReportEntity buildEntity(UserEntity reportedUser, UserEntity reporter,
                                         UserReportType type, String justification) {
        var entity = new UserReportEntity();
        entity.setId(UUID.randomUUID());
        entity.setReportedUser(reportedUser);
        entity.setReporter(reporter);
        entity.setReportType(type);
        entity.setStatus(ReportStatus.PENDING);
        entity.setJustification(justification);
        return entity;
    }

    @Nested
    class ToDomain {

        @Test
        void shouldConvertEntityToDomain() {
            var entity = buildEntity(reportedUserEntity, reporterEntity, UserReportType.SPAM, "Ceci est un spam");

            var domain = UserReportEntityMapper.toDomain(entity);

            assertNotNull(domain);
            assertEquals(reportedUserEntity.getId(), domain.reportedUserId());
            assertEquals(reporterEntity.getId(), domain.reporterId());
            assertEquals(UserReportType.SPAM, domain.reportType());
            assertEquals(ReportStatus.PENDING, domain.status());
            assertEquals("Ceci est un spam", domain.justification());
            assertNull(domain.reviewedAt());
            assertNull(domain.reviewedById());
        }

        @Test
        void shouldHandleNullReporter() {
            var entity = buildEntity(reportedUserEntity, null, UserReportType.HARASSMENT, "Justification");

            var domain = UserReportEntityMapper.toDomain(entity);

            assertNull(domain.reporterId());
        }

        @Test
        void shouldHandleNullReviewedBy() {
            var entity = buildEntity(reportedUserEntity, reporterEntity, UserReportType.FRAUD, "Arnaque");
            entity.setReviewedBy(null);

            var domain = UserReportEntityMapper.toDomain(entity);

            assertNull(domain.reviewedById());
        }

        @Test
        void shouldMapReviewedByWhenPresent() {
            var entity = buildEntity(reportedUserEntity, reporterEntity, UserReportType.SPAM, "Spam confirmé");
            entity.setStatus(ReportStatus.VALIDATED);
            entity.setReviewedBy(reviewerEntity);
            entity.setReviewedAt(now);

            var domain = UserReportEntityMapper.toDomain(entity);

            assertEquals(reviewerEntity.getId(), domain.reviewedById());
            assertEquals(now, domain.reviewedAt());
            assertEquals(ReportStatus.VALIDATED, domain.status());
        }

        @Test
        void shouldMapFromViewToDomain() {
            UserReportDetailsView view = new UserReportDetailsView() {
                public java.util.UUID getId() { return UUID.randomUUID(); }
                public java.util.UUID getReportedUserId() { return reportedUserEntity.getId(); }
                public String getReportedUsername() { return reportedUserEntity.getUsername(); }
                public String getReporterUsername() { return reporterEntity.getUsername(); }
                public String getReportType() { return "SPAM"; }
                public String getJustification() { return "Justification"; }
                public java.time.Instant getCreatedAt() { return now; }
            };
            var details = UserReportEntityMapper.toDomain(view);
            assertNotNull(details);
            assertEquals(reportedUserEntity.getId(), details.reportedUserId());
            assertEquals(reportedUserEntity.getUsername(), details.reportedUsername());
            assertEquals(reporterEntity.getUsername(), details.reporterUsername());
            assertEquals("SPAM", details.reportType());
            assertEquals("Justification", details.justification());
            assertEquals(now, details.createdAt());
        }
    }

    @Nested
    class ToEntity {

        @Test
        void shouldConvertDomainToEntity_AllFields() {
            var id = UUID.randomUUID();
            var report = new UserReport(
                    id, reportedUserEntity.getId(), reporterEntity.getId(),
                    UserReportType.INAPPROPRIATE_CONTENT, ReportStatus.VALIDATED,
                    "Contenu inapproprié", now, now, now, reviewerEntity.getId()
            );

            var entity = UserReportEntityMapper.toEntity(report, reportedUserEntity, reporterEntity, reviewerEntity);

            assertNotNull(entity);
            assertEquals(id, entity.getId());
            assertEquals(reportedUserEntity, entity.getReportedUser());
            assertEquals(reporterEntity, entity.getReporter());
            assertEquals(UserReportType.INAPPROPRIATE_CONTENT, entity.getReportType());
            assertEquals(ReportStatus.VALIDATED, entity.getStatus());
            assertEquals("Contenu inapproprié", entity.getJustification());
            assertEquals(now, entity.getReviewedAt());
            assertEquals(reviewerEntity, entity.getReviewedBy());
        }

        @Test
        void shouldConvertDomainToEntity_WithNullReviewedByAndReviewedAt() {
            var id = UUID.randomUUID();
            var report = new UserReport(
                    id, reportedUserEntity.getId(), reporterEntity.getId(),
                    UserReportType.SPAM, ReportStatus.PENDING,
                    "Justification", now, now, null, null
            );

            var entity = UserReportEntityMapper.toEntity(report, reportedUserEntity, reporterEntity, null);

            assertNotNull(entity);
            assertEquals(id, entity.getId());
            assertEquals(reportedUserEntity, entity.getReportedUser());
            assertEquals(reporterEntity, entity.getReporter());
            assertEquals(UserReportType.SPAM, entity.getReportType());
            assertEquals(ReportStatus.PENDING, entity.getStatus());
            assertEquals("Justification", entity.getJustification());
            assertNull(entity.getReviewedAt());
            assertNull(entity.getReviewedBy());
        }

        @Test
        void shouldSetNullReporter_WhenReporterIsNull() {
            var id = UUID.randomUUID();
            var report = new UserReport(
                    id, reportedUserEntity.getId(), null,
                    UserReportType.SPAM, ReportStatus.PENDING,
                    "Justification", now, now, null, null
            );

            var entity = UserReportEntityMapper.toEntity(report, reportedUserEntity, null, null);

            assertNotNull(entity);
            assertEquals(id, entity.getId());
            assertEquals(reportedUserEntity, entity.getReportedUser());
            assertNull(entity.getReporter());
            assertEquals(UserReportType.SPAM, entity.getReportType());
            assertEquals(ReportStatus.PENDING, entity.getStatus());
            assertEquals("Justification", entity.getJustification());
            assertNull(entity.getReviewedAt());
            assertNull(entity.getReviewedBy());
        }
    }
}