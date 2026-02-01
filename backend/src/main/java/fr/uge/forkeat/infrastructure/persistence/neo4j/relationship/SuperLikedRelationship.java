package fr.uge.forkeat.infrastructure.persistence.neo4j.relationship;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.*;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;
import java.util.UUID;

@RelationshipProperties
public class SuperLikedRelationship {

    @Id
    @GeneratedValue
    private Long id;
    private LocalDateTime date;
    private Integer amount;
    private UUID transactionId;

    @TargetNode
    private RecipeNode recipe;

    public SuperLikedRelationship() {
    }

    public SuperLikedRelationship(RecipeNode recipe, LocalDateTime date, Integer amount, UUID transactionId) {
        this.recipe = recipe;
        this.date = date;
        this.amount = amount;
        this.transactionId = transactionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public RecipeNode getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeNode recipe) {
        this.recipe = recipe;
    }
}
