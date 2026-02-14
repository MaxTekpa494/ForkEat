package fr.uge.forkeat.infrastructure.persistence.neo4j.node;

import fr.uge.forkeat.infrastructure.persistence.neo4j.relationship.*;
import io.jsonwebtoken.lang.InstantiationException;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Node("User")
public class UserNode {

    @Id
    private UUID id;
    private String username;
    private String email;

    // Flag pour marquer les utilisateurs supprimés (conservés pour les SUPER_LIKED)
    private boolean deleted = false;

    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    private Set<FollowsRelationship> following = new HashSet<>();

    @Relationship(type = "PUBLISHED", direction = Relationship.Direction.OUTGOING)
    private Set<PublishedRelationship> publishedRecipes = new HashSet<>();

    @Relationship(type = "FOLLOWS_RECIPE", direction = Relationship.Direction.OUTGOING)
    private Set<FollowsRecipeRelationship> followedRecipes = new HashSet<>();

    @Relationship(type = "LIKED", direction = Relationship.Direction.OUTGOING)
    private Set<LikedRelationship> likedRecipes = new HashSet<>();

    @Relationship(type = "SUPER_LIKED", direction = Relationship.Direction.OUTGOING)
    private Set<SuperLikedRelationship> superLikedRecipes = new HashSet<>();

    public UserNode() {
    }

    public UserNode(UUID id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void addLike(RecipeNode recipe) {
        var relation = new LikedRelationship(recipe, Instant.now());
        likedRecipes.add(relation);
    }
}
