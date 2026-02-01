package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recipe_allergens", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"recipe_id", "allergen_id"})
})
public class RecipeAllergenEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private RecipeEntity recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergen_id", nullable = false)
    private AllergenEntity allergen;

    public RecipeAllergenEntity() {}

    public RecipeAllergenEntity(RecipeEntity recipe, AllergenEntity allergen) {
        this.recipe = recipe;
        this.allergen = allergen;
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

    public void setId(UUID id){ this.id = id; }

    public RecipeEntity getRecipe() {
        return recipe;
    }

    void setRecipe(RecipeEntity recipe) {
        this.recipe = recipe;
    }

    public void setId(UUID id){
        this.id = id;
    }

    public AllergenEntity getAllergen() {
        return allergen;
    }

    public void setAllergen(AllergenEntity allergen) {
        this.allergen = allergen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipeAllergenEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
