
package fr.uge.forkeat.presentation.web.dto;

import fr.uge.forkeat.service.model.user.UserModerationActionType;

import java.util.UUID;

public record UserModerationRequest(
    UserModerationActionType action,
    String justification,
    UUID userId,
    int suspensionDays,
    int suspensionHours
) {}



