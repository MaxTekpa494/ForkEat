package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.transaction.TransactionStatus; // Import TransactionStatus
import fr.uge.forkeat.service.model.transaction.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name="transactions")
public class TransactionEntity {
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id")
    private WalletEntity sourceWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_wallet_id")
    private WalletEntity destinationWallet;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "stripe_transaction_id")
    private String stripeTransactionID;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private TransactionStatus status; // Utile pour le retrait avec Stripe

    @Column(name = "created_at")
    private Instant createdAt;

    public TransactionEntity(){}

    public TransactionEntity(WalletEntity sourceWallet, WalletEntity destinationWallet,
                             Long amount, String stripeTransactionID,
                             TransactionType transactionType, TransactionStatus status, Instant createdAt) {
        this.sourceWallet = sourceWallet;
        this.destinationWallet = destinationWallet;
        this.amount = amount;
        this.stripeTransactionID = stripeTransactionID;
        this.transactionType = transactionType;
        this.status = status; // Initialize status
        this.createdAt = createdAt;
    }


    @PrePersist
    private void onCreate(){
        if(id == null){
            id = UUID.randomUUID();
        }
        if(createdAt == null) {
            createdAt = Instant.now();
        }
        if(status == null) {
            status = TransactionStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        if(amount < 0){
            throw new IllegalArgumentException("Amount transaction must be positif");
        }
        this.amount = amount;
    }

    public String getStripeTransactionID() {
        return stripeTransactionID;
    }

    public void setStripeTransactionID(String stripeTransactionID) {
        this.stripeTransactionID = stripeTransactionID;
    }

    public WalletEntity getSourceWallet() {
        return sourceWallet;
    }

    public void setSourceWallet(WalletEntity sourceWallet) {
        this.sourceWallet = sourceWallet;
    }

    public WalletEntity getDestinationWallet() {
        return destinationWallet;
    }

    public void setDestinationWallet(WalletEntity destinationWallet) {
        this.destinationWallet = destinationWallet;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}