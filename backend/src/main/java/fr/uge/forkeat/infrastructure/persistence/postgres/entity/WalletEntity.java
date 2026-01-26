package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "balance", nullable = false)
    private Long balance = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    public WalletEntity(){}

    public WalletEntity(Long balance, UserEntity user) {
        this.balance = balance;
        this.user = user;
    }

    @PrePersist
    private void onCreate(){
        if (id == null) {
            id = UUID.randomUUID();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate(){
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserEntity getUser() {
        return user;
    }

    void setUser(UserEntity user) {
        this.user = user;
    }


    /**
     * Crédite le wallet (recharge ou redistribution)
     * @throws IllegalArgumentException si amount négatif
     */
    public void credit(long amount){
        if(amount < 0){
            throw new IllegalArgumentException("Cannot credit negative amount");
        }
        balance += amount;
    }


    /**
     * Débite le wallet (super-like, retrait)
     * @throws IllegalStateException si solde insuffisant
     * @throws IllegalArgumentException si amount négatif
     */
    public void debit(long amount){
        if(amount < 0){
            throw new IllegalArgumentException("Cannot debit negative amount");
        }
        if(balance < amount){
            throw new IllegalStateException("Insufficient balance");
        }
        balance -= amount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WalletEntity)) return false;
        var wallet = (WalletEntity) o;
        return id != null && id.equals(wallet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
