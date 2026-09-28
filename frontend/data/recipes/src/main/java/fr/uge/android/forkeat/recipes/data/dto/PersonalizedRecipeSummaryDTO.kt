package fr.uge.android.forkeat.recipes.data.dto

import java.util.UUID

data class PersonalizedRecipeSummaryDTO(
    val id: UUID,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val preparationMinutes: Int,
    val authorUsername: String,
    val likeCount: Long,
    val superLikeCount: Long,
    val followCount: Long,
    val likedByCurrentUser: Boolean,
    val superLikedByCurrentUser: Boolean,
    val followedByCurrentUser: Boolean
)
