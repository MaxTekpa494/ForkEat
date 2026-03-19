package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserModerationActionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import fr.uge.forkeat.service.model.user.UserReportType;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserModerationActionEntityMapperTest {

    private UserEntity userEntity;
    private UserEntity moderatorEntity;
    private UserReportEntity relatedReportEntity;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        userEntity = new UserEntity();
        userEntity.setId(UUID.randomUUID());
        userEntity.setUsername("target");
        userEntity.setFirstName("Alice");
        userEntity.setLastName("Martin");
        userEntity.setEmail("target@test.com");
        userEntity.setPassword("hashed");
        userEntity.setRole(UserRole.MEMBER);
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setAuthMode(AuthMode.LOCAL);

        moderatorEntity = new UserEntity();
        moderatorEntity.setId(UUID.randomUUID());
        moderatorEntity.setUsername("moderator");
        moderatorEntity.setFirstName("Bob");
        moderatorEntity.setLastName("Dupont");
        moderatorEntity.setEmail("moderator@test.com");
        moderatorEntity.setPassword("hashed");
        moderatorEntity.setRole(UserRole.MODERATOR);
        moderatorEntity.setStatus(UserStatus.ACTIVE);
        moderatorEntity.setAuthMode(AuthMode.LOCAL);

        relatedReportEntity = new UserReportEntity();
        relatedReportEntity.setId(UUID.randomUUID());
        relatedReportEntity.setReportedUser(userEntity);
        relatedReportEntity.setReporter(moderatorEntity);
        relatedReportEntity.setReportType(UserReportType.HARASSMENT);
        relatedReportEntity.setStatus(ReportStatus.PENDING);
        relatedReportEntity.setJustification("Comportement abusif");
    }

    private UserModerationActionEntity buildEntity(UserReportEntity relatedReport, Instant suspendedUntil) {
        var entity = new UserModerationActionEntity();
        entity.setId(UUID.randomUUID());
        entity.setUser(userEntity);
        entity.setModerator(moderatorEntity);
        entity.setModerationActionType(UserModerationActionType.SUSPENDED);
        entity.setJustification("Suspendu 7 jours");
        entity.setUpdatedAt(now);
        entity.setSuspendedUntil(suspendedUntil);
        entity.setRelatedReport(relatedReport);
        return entity;
    }

    @Nested
    class ToDomain {

        @Test
        void shouldConvertEntityToDomain() {
            var suspendedUntil = now.plusSeconds(7 * 24 * 3600L);
            var entity = buildEntity(relatedReportEntity, suspendedUntil);

            var domain = UserModerationActionEntityMapper.toDomain(entity);

            assertNotNull(domain);
            assertEquals(entity.getId(), domain.id());
            assertEquals(userEntity.getId(), domain.userId());
            assertEquals(moderatorEntity.getId(), domain.moderatorId());
            assertEquals(UserModerationActionType.SUSPENDED, domain.moderationActionType());
            assertEquals("Suspendu 7 jours", domain.justification());
            assertEquals(suspendedUntil, domain.suspendedUntil());
            assertEquals(relatedReportEntity.getId(), domain.relatedReportId());
        }
    }

    @Nested
    class ToEntity {

        @Test
        void shouldConvertDomainToEntity() {
            var suspendedUntil = now.plusSeconds(3600);
            var action = new UserModerationAction(
                    UUID.randomUUID(),
                    userEntity.getId(),
                    moderatorEntity.getId(),
                    UserModerationActionType.WARNING,
                    "Avertissement",
                    now,
                    now,
                    suspendedUntil,
                    relatedReportEntity.getId()
            );

            var entity = UserModerationActionEntityMapper.toEntity(action, userEntity, moderatorEntity, relatedReportEntity);

            assertNotNull(entity);
            assertEquals(userEntity, entity.getUser());
            assertEquals(moderatorEntity, entity.getModerator());
            assertEquals(UserModerationActionType.WARNING, entity.getModerationActionType());
            assertEquals("Avertissement", entity.getJustification());
            assertEquals(suspendedUntil, entity.getSuspendedUntil());
            assertEquals(relatedReportEntity, entity.getRelatedReport());
        }
    }
}

