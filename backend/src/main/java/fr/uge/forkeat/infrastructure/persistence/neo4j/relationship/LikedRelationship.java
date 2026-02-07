package fr.uge.forkeat.infrastructure.persistence.neo4j.relationship;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.Instant;

@RelationshipProperties
public class LikedRelationship {

    @Id
    @GeneratedValue
    private Long id;
    private Instant date;
    @TargetNode
    private RecipeNode recipe;

    public LikedRelationship() {
    }

    public LikedRelationship(RecipeNode recipe, Instant date) {
        this.recipe = recipe;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public RecipeNode getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeNode recipe) {
        this.recipe = recipe;
    }
}
