package fr.uge.forkeat.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.UUID;

@Node("Recipe")
public class RecipeNode {

    @Id
    private UUID id;
    private String title;
    private Instant deletedAt; // Horodatage de suppression (null = vivante) pour les recettes avec SUPER_LIKED

    @Relationship(type = "IS_VARIANT_OF", direction = Relationship.Direction.OUTGOING)
    private RecipeNode parentRecipe;

    public RecipeNode() {
    }

    public RecipeNode(UUID id, String title) {
        this.id = id;
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public RecipeNode getParentRecipe() {
        return parentRecipe;
    }

    public void setParentRecipe(RecipeNode parentRecipe) {
        this.parentRecipe = parentRecipe;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
