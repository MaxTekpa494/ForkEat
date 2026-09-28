package fr.uge.android.forkeat.moderator.data.dto

import java.util.UUID

data class UserModerationRequest(
  val action: String,
  val justification: String,
  val userId: UUID,
  val suspensionDays: Int,
  val suspensionHours: Int
)
