package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "recipes")
public class RecipeEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(length = 50)
    private String source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private RecipeEntity parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @Column(name = "preparation_minutes")
    private int preparationMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_by_step_instructions", nullable = false, columnDefinition = "jsonb")
    private List<RecipeStep> stepByStepInstructions = new ArrayList<>();

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipeStatus status = RecipeStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dietary_flag", nullable = false, columnDefinition = "jsonb") // Voilà pour les tests sont importants, ici j'avais oublié d'initialiser
    // Le Map ce qui fait que quand on veut ajouter des dietary, ça plante jusqu'à ce que je me rende compte
    private HashMap<String, Boolean> dietaryFlag = new HashMap<>(); // Ici, je reflechis à mettre une List<Record> à la place

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredientEntity> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeAllergenEntity> allergens = new ArrayList<>();

    public RecipeEntity() {}

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public RecipeEntity getParent() {
        return parent;
    }

    public void setParent(RecipeEntity parent) {
        this.parent = parent;
    }

    public UserEntity getAuthor() {
        return author;
    }

    public void setAuthor(UserEntity author) {
        this.author = author;
    }

    public int getPreparationMinutes() {
        return preparationMinutes;
    }

    public void setPreparationMinutes(int preparationMinutes) {
        this.preparationMinutes = preparationMinutes;
    }

    public List<RecipeStep> getStepByStepInstructions() {
        return stepByStepInstructions;
    }

    public void setStepByStepInstructions(List<RecipeStep> stepByStepInstructions) {
        this.stepByStepInstructions = List.copyOf(stepByStepInstructions); // Est-ce qu'on peut faire ça ci ?
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public RecipeStatus getStatus() {
        return status;
    }

    public void setStatus(RecipeStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Map<String, Boolean> getDietaryFlag() {
        return dietaryFlag;
    }

    public void setDietaryFlag(Map<String, Boolean> dietaryFlag) {
        dietaryFlag.forEach((flag, value) -> {
            this.dietaryFlag.put(flag, value);
        });
    }


    public List<RecipeIngredientEntity> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredientEntity> ingredients){
        ingredients.forEach(this::addIngredient);
    }
    public void addIngredient(RecipeIngredientEntity ingredient) {
        ingredients.add(ingredient);
        ingredient.setRecipe(this);
    }

    public void removeIngredient(RecipeIngredientEntity ingredient) {
        ingredients.remove(ingredient);
        ingredient.setRecipe(null);
    }

    public List<RecipeAllergenEntity> getAllergens() {
        return allergens;
    }


    public void setAllergens(List<RecipeAllergenEntity> allergens){
        allergens.forEach(this::addAllergen);
    }
    public void addAllergen(RecipeAllergenEntity allergen) {
        allergens.add(allergen);
        allergen.setRecipe(this);
    }

    public void removeAllergen(RecipeAllergenEntity allergen) {
        allergens.remove(allergen);
        allergen.setRecipe(null);
    }
}
