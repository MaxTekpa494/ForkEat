package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.model.UserStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="users")
public class UserEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "auth_mode", nullable = false)
    private AuthMode authMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private BankInfoEntity bankInfo;

    public UserEntity(){}

    public UserEntity(String username, String firstName, String lastName, String password, String email, UserRole role, UserStatus status, AuthMode authMode) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.email = email;
        this.role = role;
        this.status = status;
        this.authMode = authMode;
    }

    // Voir comment lui verser tout son argent quand il se déconnecte
    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            // Sinon on ne veut pas de eager, il faut que ça soit uni-directionnel et dans ce cas il faut
            // supprimer le champs wallet de la classe UserWallet
            optional = false
    )
    private WalletEntity wallet;


    @PrePersist
    private void create(){
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate(){
        updatedAt = Instant.now();
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setId(UUID id){
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public AuthMode getAuthMode() {
        return authMode;
    }

    public void setAuthMode(AuthMode authMode) {
        this.authMode = authMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BankInfoEntity getBankInfo() {
        return bankInfo;
    }

    public void setBankInfo(BankInfoEntity bankInfo) {
        if(this.bankInfo != null){
            this.bankInfo.setUser(null);
        }
        this.bankInfo = bankInfo;
        if(this.bankInfo != null){
            bankInfo.setUser(this);
        }
    }

    public WalletEntity getWallet() {
        return wallet;
    }

    public void setWallet(WalletEntity wallet){
        if(this.wallet != null){
            this.wallet.setUser(null);
        }
        this.wallet = wallet;
        if(this.wallet != null){
            wallet.setUser(this);
        }
    }

    public UUID getId() {
        return id;
    }

}
