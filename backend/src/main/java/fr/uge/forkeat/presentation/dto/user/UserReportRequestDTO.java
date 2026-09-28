package fr.uge.forkeat.presentation.dto.user;

import fr.uge.forkeat.service.model.user.UserReportType;

public record UserReportRequestDTO(UserReportType reportType, String justification) {
    public UserReportRequestDTO {
        if (reportType == null) {
            throw new IllegalArgumentException("reportType is required");
        }
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("justification cannot be empty");
        }
    }
}