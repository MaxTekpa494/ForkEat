package fr.uge.forkeat.infrastructure.persistence.neo4j.relationship;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.UserNode;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.Instant;

@RelationshipProperties
public class FollowsRelationship {

    @Id
    @GeneratedValue
    private String id;
    private Instant since;
    @TargetNode
    private UserNode followedUser;

    public FollowsRelationship() {
    }

    public FollowsRelationship(UserNode followedUser, Instant since) {
        this.followedUser = followedUser;
        this.since = since;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getSince() {
        return since;
    }

    public void setSince(Instant since) {
        this.since = since;
    }

    public UserNode getFollowedUser() {
        return followedUser;
    }

    public void setFollowedUser(UserNode followedUser) {
        this.followedUser = followedUser;
    }
}
