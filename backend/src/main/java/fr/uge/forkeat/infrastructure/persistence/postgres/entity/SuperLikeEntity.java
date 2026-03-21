package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "super_likes")
public class SuperLikeEntity {
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "promotion_id")
    private UUID promotionId;

    @Column(name = "is_bonus_free", nullable = false)
    private boolean isBonusFree;

    @Column(name = "redist_amount_cents", nullable = false)
    private long redistAmountCents;

    @PrePersist
    private void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public SuperLikeEntity() {}

    public SuperLikeEntity(UUID userId, UUID recipeId, long amount, UUID promotionId, boolean isBonusFree, long redistAmountCents) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.amount = amount;
        this.promotionId = promotionId;
        this.isBonusFree = isBonusFree;
        this.redistAmountCents = redistAmountCents;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getRecipeId() { return recipeId; }
    public void setRecipeId(UUID recipeId) { this.recipeId = recipeId; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UUID getPromotionId() { return promotionId; }
    public void setPromotionId(UUID promotionId) { this.promotionId = promotionId; }

    public boolean isBonusFree() { return isBonusFree; }
    public void setBonusFree(boolean bonusFree) { isBonusFree = bonusFree; }

    public long getRedistAmountCents() { return redistAmountCents; }
    public void setRedistAmountCents(long redistAmountCents) { this.redistAmountCents = redistAmountCents; }
}
