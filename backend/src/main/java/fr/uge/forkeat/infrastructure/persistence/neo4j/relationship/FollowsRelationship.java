package fr.uge.forkeat.infrastructure.persistence.neo4j.relationship;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.*;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

@RelationshipProperties
public class FollowsRelationship {

    @Id
    @GeneratedValue
    private Long id;
    private LocalDateTime since;
    @TargetNode
    private UserNode followedUser;

    public FollowsRelationship() {
    }

    public FollowsRelationship(UserNode followedUser, LocalDateTime since) {
        this.followedUser = followedUser;
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

    public UserNode getFollowedUser() {
        return followedUser;
    }

    public void setFollowedUser(UserNode followedUser) {
        this.followedUser = followedUser;
    }
}
