package fr.uge.android.forkeat.network.dto.admin

import com.google.gson.annotations.SerializedName
import fr.uge.android.forkeat.network.dto.UserResource

data class AdminUsersListResponse(
    @SerializedName("resources")
    val items: List<UserResource>,
    val total: Long
)
