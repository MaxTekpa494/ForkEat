package fr.uge.forkeat.infrastructure.persistence.neo4j.relationship;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.Instant;

@RelationshipProperties
public class FollowsRecipeRelationship {

    @Id
    @GeneratedValue
    private Long id;
    private Instant since;
    @TargetNode
    private RecipeNode recipe;

    public FollowsRecipeRelationship() {
    }

    public FollowsRecipeRelationship(RecipeNode recipe, Instant since) {
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

    public Instant getSince() {
        return since;
    }

    public void setSince(Instant since) {
        this.since = since;
    }

    public RecipeNode getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeNode recipe) {
        this.recipe = recipe;
    }
}
