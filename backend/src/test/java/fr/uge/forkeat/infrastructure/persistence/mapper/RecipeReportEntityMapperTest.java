package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeReportDetailsView;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.RecipeReport;
import fr.uge.forkeat.service.model.recipe.RecipeReportType;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeReportEntityMapperTest {

    private RecipeEntity recipeEntity;
    private UserEntity reporterEntity;
    private UserEntity reviewerEntity;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

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

        recipeEntity = new RecipeEntity();
        recipeEntity.setId(UUID.randomUUID());
        recipeEntity.setTitle("Tarte aux pommes");
        recipeEntity.setSummary("Une délicieuse tarte");
        recipeEntity.setAuthor(reporterEntity);
        recipeEntity.setStatus(RecipeStatus.PUBLISHED);
        recipeEntity.setStepByStepInstructions(List.of());
        recipeEntity.setCreatedAt(now);
        recipeEntity.setUpdatedAt(now);
    }

    private RecipeReportEntity buildEntity(RecipeEntity recipe, UserEntity reporter,
                                           RecipeReportType type, String justification) {
        var entity = new RecipeReportEntity();
        entity.setId(UUID.randomUUID());
        entity.setRecipe(recipe);
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
            var entity = buildEntity(recipeEntity, reporterEntity, RecipeReportType.SPAM, "Ceci est un spam");

            var domain = RecipeReportEntityMapper.toDomain(entity);

            assertNotNull(domain);
            assertEquals(recipeEntity.getId(), domain.recipeId());
            assertEquals(reporterEntity.getId(), domain.reporterId());
            assertEquals(RecipeReportType.SPAM, domain.reportType());
            assertEquals(ReportStatus.PENDING, domain.status());
            assertEquals("Ceci est un spam", domain.justification());
            assertNull(domain.reviewedAt());
            assertNull(domain.reviewedById());
        }

        @Test
        void shouldHandleNullReporter() {
            var entity = buildEntity(recipeEntity, null, RecipeReportType.INAPPROPRIATE, "Justification");

            var domain = RecipeReportEntityMapper.toDomain(entity);

            assertNull(domain.reporterId());
        }

        @Test
        void shouldHandleNullReviewedBy() {
            var entity = buildEntity(recipeEntity, reporterEntity, RecipeReportType.DANGEROUS, "Dangereux");
            entity.setReviewedBy(null);

            var domain = RecipeReportEntityMapper.toDomain(entity);

            assertNull(domain.reviewedById());
        }

        @Test
        void shouldMapReviewedByWhenPresent() {
            var entity = buildEntity(recipeEntity, reporterEntity, RecipeReportType.SPAM, "Spam confirmé");
            entity.setStatus(ReportStatus.VALIDATED);
            entity.setReviewedBy(reviewerEntity);
            entity.setReviewedAt(now);

            var domain = RecipeReportEntityMapper.toDomain(entity);

            assertEquals(reviewerEntity.getId(), domain.reviewedById());
            assertEquals(now, domain.reviewedAt());
            assertEquals(ReportStatus.VALIDATED, domain.status());
        }

        @Test
        void shouldMapFromViewToDomain() {
            RecipeReportDetailsView view = new RecipeReportDetailsView() {
                public java.util.UUID getId() { return UUID.randomUUID(); }
                public java.util.UUID getRecipeId() { return recipeEntity.getId(); }
                public String getRecipeTitle() { return recipeEntity.getTitle(); }
                public String getRecipeImageUrl() { return "image.jpg"; }
                public String getReporterUsername() { return reporterEntity.getUsername(); }
                public String getReportType() { return "SPAM"; }
                public String getJustification() { return "Justification"; }
                public java.time.Instant getCreatedAt() { return now; }
            };
            var details = RecipeReportEntityMapper.toDomain(view);
            assertNotNull(details);
            assertEquals(recipeEntity.getId(), details.recipeId());
            assertEquals(recipeEntity.getTitle(), details.recipeTitle());
            assertEquals("image.jpg", details.recipeImageUrl());
            assertEquals(reporterEntity.getUsername(), details.reporterUsername());
            assertEquals("SPAM", details.reportType());
            assertEquals("Justification", details.justification());
            assertEquals(now, details.createdAt());
        }
    }

    @Nested
    class ToEntity {

        @Test
        void shouldConvertDomainToEntity() {
            var report = new RecipeReport(
                    UUID.randomUUID(), recipeEntity.getId(), reporterEntity.getId(),
                    RecipeReportType.COPYRIGHT, ReportStatus.PENDING,
                    "Contenu copié", now, null, null
            );

            var entity = RecipeReportEntityMapper.toEntity(report, recipeEntity, reporterEntity);

            assertNotNull(entity);
            assertEquals(recipeEntity, entity.getRecipe());
            assertEquals(reporterEntity, entity.getReporter());
            assertEquals(RecipeReportType.COPYRIGHT, entity.getReportType());
            assertEquals(ReportStatus.PENDING, entity.getStatus());
            assertEquals("Contenu copié", entity.getJustification());
        }

        @Test
        void shouldSetNullReporter_WhenReporterIsNull() {
            var report = new RecipeReport(
                    UUID.randomUUID(), recipeEntity.getId(), null,
                    RecipeReportType.SPAM, ReportStatus.PENDING,
                    "Justification", now, null, null
            );

            var entity = RecipeReportEntityMapper.toEntity(report, recipeEntity, null);

            assertNull(entity.getReporter());
        }
    }
}
