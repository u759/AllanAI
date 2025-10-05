package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists authentication session information locally using SharedPreferences.
 * Stores the bearer token and basic user profile details so the app can
 * identify the logged in user between launches without caching passwords.
 */
class AuthLocalDataSource(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun saveSession(session: AuthSession) {
        prefs.edit().apply {
            putString(KEY_TOKEN, session.token)
            putString(KEY_TOKEN_TYPE, session.tokenType)
            putString(KEY_EXPIRES_AT, session.expiresAtIso)
            putString(KEY_USER_ID, session.user.id)
            putString(KEY_USERNAME, session.user.username)
            putString(KEY_EMAIL, session.user.email)
        }.apply()
    }

    fun updateUser(user: StoredUser) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
        }.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getAuthorizationHeader(): String? {
        val token = getToken() ?: return null
        val type = getTokenType()
        return if (type.isNullOrBlank()) token else "$type $token"
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getTokenType(): String? = prefs.getString(KEY_TOKEN_TYPE, DEFAULT_TOKEN_TYPE)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getTokenExpiryIso(): String? = prefs.getString(KEY_EXPIRES_AT, null)

    fun getCurrentUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getCurrentUserEmail(): String? = prefs.getString(KEY_EMAIL, null)

    companion object {
        private const val PREFS_NAME = "allan_ai_auth"
        private const val KEY_TOKEN = "token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val DEFAULT_TOKEN_TYPE = "Bearer"
    }
}

/**
 * Stored authentication session information used by [AuthLocalDataSource].
 */
data class AuthSession(
    val token: String,
    val tokenType: String,
    val expiresAtIso: String?,
    val user: StoredUser
)

data class StoredUser(
    val id: String,
    val username: String,
    val email: String
)
