package fr.uge.forkeat.presentation.dto.recipe;

import java.util.UUID;

public record DismissReportRequest(UUID recipeId, String justification) {}

