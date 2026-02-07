package fr.uge.forkeat.infrastructure.persistence.postgres.entity;


import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="bank_infos", indexes={
        @Index(name = "idx_bank_info_user", columnList = "user_id")
})
public class BankInfoEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "bank_name")
    private String bankName;
    @Column(nullable = false, length = 34)
    private String iban;
    @Column(nullable = false, length = 11)
    private String bic;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;


    public BankInfoEntity(){}

    public BankInfoEntity(String bankName, String iban, String bic, UserEntity user) {
        this.bankName = bankName;
        this.iban = iban;
        this.bic = bic;
        this.user = user;
    }


    @PrePersist
     private void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate(){
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankName() {
        return bankName;
    }


    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getIban(){
        return iban;
    }
    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UserEntity getUser() {
        return user;
    }

    // On met package ici; pour mettre à jour bankinfo il
    // faut obligatoirement passer par user car c'est une composition
    void setUser(UserEntity user) {
        this.user = user;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankInfoEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
