package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recipe_rag")
public class RecipeRagEntity {
  @Id
  @Column(columnDefinition = "UUID")
  private UUID id;

  @Column(name = "document_text", nullable = false, columnDefinition = "TEXT")
  private String documentText;

  @Transient
  private float[] embedding;

  @Column(name = "title", nullable = false, columnDefinition = "TEXT")
  private String title;

  @Column(name = "ingredients", columnDefinition = "TEXT")
  private String ingredients;

  @Column(name = "dietaries", columnDefinition = "TEXT")
  private String dietaries;

  @Column(name = "allergens", columnDefinition = "TEXT")
  private String allergens;

  @Column(name = "ready_in_minutes")
  private Integer readyInMinutes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipe_id", nullable = false, unique = true)
  private RecipeEntity recipe;


  public RecipeRagEntity() {}


  public RecipeRagEntity(String documentText, float[] embedding, String title, String ingredients, String dietaries, String allergens, Integer readyInMinutes, RecipeEntity recipe) {
    this.documentText = documentText;
    this.embedding = embedding;
    this.title = title;
    this.ingredients = ingredients;
    this.dietaries = dietaries;
    this.allergens = allergens;
    this.readyInMinutes = readyInMinutes;
    this.recipe = recipe;
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
  private void onUpdate() {
    updatedAt = Instant.now();
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getDocumentText() {
    return documentText;
  }

  public void setDocumentText(String documentText) {
    this.documentText = documentText;
  }

  public float[] getEmbedding() {
    return embedding;
  }
  public void setEmbedding(float[] embedding) {
    this.embedding = embedding;
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getIngredients() {
    return ingredients;
  }
  public void setIngredients(String ingredients) {
    this.ingredients = ingredients;
  }
  public String getDietaries() {
    return dietaries;
  }
  public void setDietaries(String dietaries) {
    this.dietaries = dietaries;
  }
  public String getAllergens() {
    return allergens;
  }
  public void setAllergens(String allergens) {
    this.allergens = allergens;
  }
  public Integer getReadyInMinutes() {
    return readyInMinutes;
  }
  public void setReadyInMinutes(Integer readyInMinutes) {
    this.readyInMinutes = readyInMinutes;
  }
  public RecipeEntity getRecipe() {
    return recipe;
  }
  public void setRecipe(RecipeEntity recipe) {
    this.recipe = recipe;
  }

  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

}
