package fr.uge.android.forkeat.network.dto.admin

import fr.uge.android.forkeat.network.dto.UserResource

data class AdminUsersListResponse(
    val resources: List<UserResource>,
    val total: Int
)
