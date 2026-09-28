package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_wallet_transactions")
public class PlatformWalletTransactionEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "wallet_type", nullable = false)
    private String walletType;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "reference_id", columnDefinition = "UUID")
    private UUID referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public PlatformWalletTransactionEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getWalletType() { return walletType; }
    public void setWalletType(String walletType) { this.walletType = walletType; }

    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
