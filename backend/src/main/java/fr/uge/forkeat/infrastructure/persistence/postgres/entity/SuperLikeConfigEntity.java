package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "super_like_config")
public class SuperLikeConfigEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "earnings_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal earningsRatio;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public SuperLikeConfigEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getPriceCents() { return priceCents; }
    public void setPriceCents(Long priceCents) { this.priceCents = priceCents; }

    public BigDecimal getEarningsRatio() { return earningsRatio; }
    public void setEarningsRatio(BigDecimal earningsRatio) { this.earningsRatio = earningsRatio; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
