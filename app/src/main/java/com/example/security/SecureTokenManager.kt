package com.example.security

import android.content.Context
import android.content.SharedPreferences

class SecureTokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "globalcorex_secure_session"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "authenticated_user_id"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_IDENTIFIER = "saved_login_identifier"
    }

    fun saveSession(userId: String, token: String, rememberMe: Boolean) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_AUTH_TOKEN, token)
            .putBoolean(KEY_REMEMBER_ME, rememberMe)
            .apply()
    }

    fun saveIdentifier(identifier: String) {
        prefs.edit().putString(KEY_SAVED_IDENTIFIER, identifier).apply()
    }

    fun getSavedIdentifier(): String? = prefs.getString(KEY_SAVED_IDENTIFIER, null)

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun getAuthenticatedUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, true)

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .apply()
    }
}
