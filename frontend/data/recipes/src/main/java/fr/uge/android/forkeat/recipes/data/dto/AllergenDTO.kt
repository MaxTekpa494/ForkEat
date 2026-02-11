package fr.uge.android.forkeat.recipes.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AllergenDTO(
  val id: String,
  val name: String,
  val severity: String
)

