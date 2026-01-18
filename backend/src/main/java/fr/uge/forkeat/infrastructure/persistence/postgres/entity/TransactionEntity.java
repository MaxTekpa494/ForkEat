package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.TransactionType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name="transaction")
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
    @Column(name = "type", nullable = false)
    private TransactionType transactionType;
    @Column(name = "created_at")
    private Instant createdAt;

    public TransactionEntity(){}

    public TransactionEntity(WalletEntity sourceWallet, WalletEntity destinationWallet,
                             Long amount, String stripeTransactionID,
                             TransactionType transactionType, Instant createdAt) {
        this.sourceWallet = sourceWallet;
        this.destinationWallet = destinationWallet;
        this.amount = amount;
        this.stripeTransactionID = stripeTransactionID;
        this.transactionType = transactionType;
        this.createdAt = createdAt;
    }


    @PrePersist
    private void onCreate(){
        if(id == null){
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
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

    void setStripeTransactionID(String stripeTransactionID) {
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
