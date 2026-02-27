package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recipe_dietary", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"recipe_id", "dietary_id"})
})
public class RecipeDietaryEntity {

  @Id
  @Column(columnDefinition = "UUID")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipe_id", nullable = false)
  private RecipeEntity recipe;

  @ManyToOne
  @JoinColumn(name = "dietary_id", nullable = false)
  private DietaryEntity dietary;

  public RecipeDietaryEntity() {}

  public RecipeDietaryEntity(RecipeEntity recipe, DietaryEntity dietary) {
    this.recipe = recipe;
    this.dietary = dietary;
  }

  public UUID getId() {
    return id;
  }
  public void setId(UUID id) {
    this.id = id;
  }
  public RecipeEntity getRecipe() {
    return recipe;
  }
  public void setRecipe(RecipeEntity recipe) {
    this.recipe = recipe;
  }
  public DietaryEntity getDietary() {
    return dietary;
  }

  public void setDietary(DietaryEntity dietary) {
    this.dietary = dietary;
  }

  @PrePersist
  private void onCreate() {
    if (id == null){
      id = UUID.randomUUID();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof RecipeDietaryEntity other)) return false;
    return Objects.equals(recipe, other.recipe) && Objects.equals(dietary, other.dietary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recipe, dietary);
  }

}
