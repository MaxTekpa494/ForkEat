package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recipe_moderation_actions")
public class RecipeModerationActionEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private RecipeEntity recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private UserEntity moderator;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action_type", nullable = false)
    private RecipeModerationActionType moderationActionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_report_id")
    private RecipeReportEntity relatedReport;

    public RecipeModerationActionEntity() {}

    @PrePersist
    private void onCreate() {
        if(id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public RecipeEntity getRecipe() { return recipe; }
    public void setRecipe(RecipeEntity recipe) { this.recipe = recipe; }

    public UserEntity getModerator() { return moderator; }
    public void setModerator(UserEntity moderator) { this.moderator = moderator; }

    public RecipeModerationActionType getModerationActionType() { return moderationActionType; }
    public void setModerationActionType(RecipeModerationActionType moderationActionType) { this.moderationActionType = moderationActionType; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public RecipeReportEntity getRelatedReport() { return relatedReport; }
    public void setRelatedReport(RecipeReportEntity relatedReport) { this.relatedReport = relatedReport; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipeModerationActionEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}