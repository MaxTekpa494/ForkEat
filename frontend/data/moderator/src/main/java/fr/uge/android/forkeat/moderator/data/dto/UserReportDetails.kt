package fr.uge.android.forkeat.moderator.data.dto

import java.util.UUID
import kotlin.time.Instant

data class UserReportDetails(
  val id: UUID,
  val reportedUserId: UUID,
  val reportedUsername: String,
  val reporterUsername: String,
  val reportType: String,
  val justification: String,
  val createdAt: Instant
)
