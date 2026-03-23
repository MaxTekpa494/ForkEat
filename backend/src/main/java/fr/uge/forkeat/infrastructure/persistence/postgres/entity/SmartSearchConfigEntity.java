package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "smart_search_config")
public class SmartSearchConfigEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "top_k", nullable = false)
    private int topK;

    @Column(name = "cost", nullable = false)
    private long cost;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public SmartSearchConfigEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public long getCost() { return cost; }
    public void setCost(long cost) { this.cost = cost; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
