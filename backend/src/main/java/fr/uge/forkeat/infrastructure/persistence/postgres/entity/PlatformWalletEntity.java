package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.WalletType;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "platform_wallets")
public class PlatformWalletEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", unique = true, nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletType type;

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public UUID getWalletId() {
        return walletId;
    }
    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }
    public WalletType getType() {
        return type;
    }
    public void setType(WalletType type) {
        this.type = type;
    }

}