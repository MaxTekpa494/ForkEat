package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_moderation_actions")
public class UserModerationActionEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private UserEntity moderator;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action_type", nullable = false)
    private UserModerationActionType moderationActionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_report_id")
    private UserReportEntity relatedReport;

    public UserModerationActionEntity() {}

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

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public UserEntity getModerator() { return moderator; }
    public void setModerator(UserEntity moderator) { this.moderator = moderator; }

    public UserModerationActionType getModerationActionType() { return moderationActionType; }
    public void setModerationActionType(UserModerationActionType moderationActionType) { this.moderationActionType = moderationActionType; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getSuspendedUntil() { return suspendedUntil; }
    public void setSuspendedUntil(Instant suspendedUntil) { this.suspendedUntil = suspendedUntil; }

    public UserReportEntity getRelatedReport() { return relatedReport; }
    public void setRelatedReport(UserReportEntity relatedReport) { this.relatedReport = relatedReport; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserModerationActionEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}