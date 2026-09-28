package fr.uge.android.forkeat.moderator.data.dto

import java.util.UUID
import kotlin.time.Instant

data class RecipeReportDetails(
  val id: UUID,
  val recipeId: UUID,
  val recipeTitle: String,
  val recipeImageUrl: String?,
  val reporterUsername: String,
  val reportType: String,
  val justification: String,
  val createdAt: Instant
)
