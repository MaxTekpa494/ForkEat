package fr.uge.forkeat.infrastructure.persistence.neo4j.relationship;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.*;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

@RelationshipProperties
public class FollowsRecipeRelationship {

    @Id
    @GeneratedValue
    private Long id;
    private LocalDateTime since;
    @TargetNode
    private RecipeNode recipe;

    public FollowsRecipeRelationship() {
    }

    public FollowsRecipeRelationship(RecipeNode recipe, LocalDateTime since) {
        this.recipe = recipe;
        this.since = since;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getSince() {
        return since;
    }

    public void setSince(LocalDateTime since) {
        this.since = since;
    }

    public RecipeNode getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeNode recipe) {
        this.recipe = recipe;
    }
}
