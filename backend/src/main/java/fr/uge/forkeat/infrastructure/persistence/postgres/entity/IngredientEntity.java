package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
public class
IngredientEntity {
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String category;
    @Column(name = "is_allergen")
    private Boolean isAllergen = false;
//    @Column(name = "created_at", nullable = false)
//    private Instant createdAt;
//    @Column(name = "update_at")
//    private Instant updateAt;

    public IngredientEntity(){}

    public IngredientEntity(String name, String category, Boolean isAllergen) {
        this.name = name;
        this.category = category;
        this.isAllergen = isAllergen;
    }

    @PrePersist
    private void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) { this.id = id; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getAllergen() {
        return isAllergen;
    }

    public void setAllergen(Boolean allergen) {
        isAllergen = allergen;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngredientEntity that)) return false;
      return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
