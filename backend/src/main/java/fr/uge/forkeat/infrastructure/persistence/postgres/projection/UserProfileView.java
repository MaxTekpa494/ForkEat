package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

import java.util.UUID;

public interface UserProfileView {
    UUID getId();
    String getUsername();
    String getFirstName();
    String getLastName();
}