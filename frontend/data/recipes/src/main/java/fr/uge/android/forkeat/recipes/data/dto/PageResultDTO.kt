package fr.uge.android.forkeat.recipes.data.dto

data class PageResultDTO<T>(
    val items: List<T>,
    val total: Long
)
