package fr.uge.forkeat.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.UUID;

@Node("Recipe")
public class RecipeNode {

    @Id
    private UUID id;
    private String title;
    private Boolean deleted = false; // Flag pour les recettes de base supprimées avec des SUPER_LIKED

    @Relationship(type = "IS_VARIANT_OF", direction = Relationship.Direction.OUTGOING)
    private RecipeNode parentRecipe;

    public RecipeNode() {
    }

    public RecipeNode(UUID id, String title) {
        this.id = id;
        this.title = title;
        this.deleted = false;
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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
