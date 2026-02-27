package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "platform_wallets")
public class PlatformWalletEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, unique = true, length = 20)
    private PlatformWalletType type;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "wallet_id", nullable = false, unique = true)
    private WalletEntity wallet;

    public PlatformWalletEntity() {}

    public PlatformWalletEntity(UUID id, PlatformWalletType type, WalletEntity wallet) {
        this.id = id;
        this.type = type;
        this.wallet = wallet;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PlatformWalletType getType() { return type; }
    public void setType(PlatformWalletType type) { this.type = type; }

    public WalletEntity getWallet() { return wallet; }
    public void setWallet(WalletEntity wallet) { this.wallet = wallet; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlatformWalletEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
