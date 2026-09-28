package fr.uge.forkeat.service.exception;

import java.util.UUID;

public class UserAlreadyReportedException extends RuntimeException {

    public UserAlreadyReportedException(UUID reportedUserId, UUID reporterId) {
        super("User " + reporterId + " has already reported user " + reportedUserId);
    }
}