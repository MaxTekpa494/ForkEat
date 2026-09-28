package fr.uge.android.forkeat.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject

class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, "Bearer " + token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    fun isAdmin(): Boolean = prefs.getString(KEY_ROLE, null) == "ADMIN"

    fun isModerator(): Boolean = prefs.getString(KEY_ROLE, null) == "MODERATOR"

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_ROLE).apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun getCurrentUsername(): String? {
        val token = getToken() ?: return null
        // Token stocké sous la forme "Bearer <header>.<payload>.<sig>"
        val jwt = if (token.startsWith("Bearer ")) token.substring(7) else token
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING),
                Charsets.UTF_8
            )
            JSONObject(payload).optString("sub").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "forkeat_auth"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_ROLE = "user_role"
    }
}
