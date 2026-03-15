package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeModerationActionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
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

class RecipeModerationActionEntityMapperTest {

    private RecipeEntity recipeEntity;
    private UserEntity moderatorEntity;
    private RecipeReportEntity relatedReportEntity;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        moderatorEntity = new UserEntity();
        moderatorEntity.setId(UUID.randomUUID());
        moderatorEntity.setUsername("mod");
        moderatorEntity.setFirstName("John");
        moderatorEntity.setLastName("Doe");
        moderatorEntity.setEmail("mod@test.com");
        moderatorEntity.setPassword("hashed");
        moderatorEntity.setRole(UserRole.MODERATOR);
        moderatorEntity.setStatus(UserStatus.ACTIVE);
        moderatorEntity.setAuthMode(AuthMode.LOCAL);

        recipeEntity = new RecipeEntity();
        recipeEntity.setId(UUID.randomUUID());
        recipeEntity.setTitle("Tarte tatin");
        recipeEntity.setSummary("Délicieuse tarte tatin");
        recipeEntity.setAuthor(moderatorEntity);
        recipeEntity.setStatus(RecipeStatus.PUBLISHED);
        recipeEntity.setStepByStepInstructions(List.of());
        recipeEntity.setCreatedAt(now);
        recipeEntity.setUpdatedAt(now);

        relatedReportEntity = new RecipeReportEntity();
        relatedReportEntity.setId(UUID.randomUUID());
        relatedReportEntity.setRecipe(recipeEntity);
        relatedReportEntity.setReporter(moderatorEntity);
        relatedReportEntity.setReportType(RecipeReportType.SPAM);
        relatedReportEntity.setStatus(ReportStatus.PENDING);
        relatedReportEntity.setJustification("Spam");
    }

    private RecipeModerationActionEntity buildEntity(RecipeEntity recipe, UserEntity moderator,
                                                    RecipeModerationActionType moderationActionType, String justification) {
        var entity = new RecipeModerationActionEntity();
        entity.setId(UUID.randomUUID());
        entity.setRecipe(recipe);
        entity.setModerator(moderator);
        entity.setModerationActionType(moderationActionType);
        entity.setJustification(justification);
        entity.setRelatedReport(relatedReportEntity);
        return entity;
    }

    @Nested
    class ToDomain {
        @Test
        void shouldConvertEntityToDomain() {
            var entity = buildEntity(recipeEntity, moderatorEntity, RecipeModerationActionType.REJECTED, "Recette incomplète");

            var domain = RecipeModerationActionEntityMapper.toDomain(entity);

            assertNotNull(domain);
            assertEquals(recipeEntity.getId(), domain.recipeId());
            assertEquals(moderatorEntity.getId(), domain.moderatorId());
            assertEquals(RecipeModerationActionType.REJECTED, domain.moderationActionType());
            assertEquals("Recette incomplète", domain.justification());
            assertNotNull(domain.relatedReportId());
        }

        @Test
        void shouldHandleNullRelatedReport() {
            var entity = buildEntity(recipeEntity, moderatorEntity, RecipeModerationActionType.REJECTED, "Termes injurieux");
            entity.setRelatedReport(null);

            var domain = RecipeModerationActionEntityMapper.toDomain(entity);

            assertNull(domain.relatedReportId());
        }

        @Test
        void shouldThrowIfJustificationEmptyAndRejected() {
            var entity = buildEntity(recipeEntity, moderatorEntity, RecipeModerationActionType.REJECTED, "");
            Exception exception = assertThrows(IllegalArgumentException.class, () -> RecipeModerationActionEntityMapper.toDomain(entity));
            assertTrue(exception.getMessage().toLowerCase().contains("justification"));
        }

        @Test
        void shouldAllowEmptyJustificationIfApproved() {
            var entity = buildEntity(recipeEntity, moderatorEntity, RecipeModerationActionType.APPROVED, "");
            assertDoesNotThrow(() -> RecipeModerationActionEntityMapper.toDomain(entity));
        }
    }

    @Nested
    class ToEntity {
        @Test
        void shouldConvertDomainToEntity() {
            var moderationAction = new RecipeModerationAction(
                    UUID.randomUUID(),
                    recipeEntity.getId(),
                    moderatorEntity.getId(),
                    RecipeModerationActionType.REJECTED,
                    "Incomplet",
                    now,
                    null,
                    null
            );

            var entity = RecipeModerationActionEntityMapper.toEntity(moderationAction, recipeEntity, moderatorEntity, null);

            assertNotNull(entity);
            assertEquals(recipeEntity, entity.getRecipe());
            assertEquals(moderatorEntity, entity.getModerator());
            assertEquals(RecipeModerationActionType.REJECTED, entity.getModerationActionType());
            assertEquals("Incomplet", entity.getJustification());
        }

        @Test
        void shouldSetNullRelatedReport_WhenRelatedReportIsNull() {
            var moderationAction = new RecipeModerationAction(
                    UUID.randomUUID(),
                    recipeEntity.getId(),
                    moderatorEntity.getId(),
                    RecipeModerationActionType.REJECTED,
                    "Pas assez d'infos",
                    now,
                    null,
                    null
            );

            var entity = RecipeModerationActionEntityMapper.toEntity(moderationAction, recipeEntity, null, null);

            assertNull(entity.getRelatedReport());
        }
    }
}
