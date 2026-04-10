package com.apppulse.floodguardai.data.repository

import android.content.Context

class AuthTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences("floodguard_auth", Context.MODE_PRIVATE)

    fun save(accessToken: String, refreshToken: String, userName: String, userEmail: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_EMAIL, userEmail)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)
    fun refreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)
    fun userName(): String? = preferences.getString(KEY_USER_NAME, null)
    fun userEmail(): String? = preferences.getString(KEY_USER_EMAIL, null)

    fun isLoggedIn(): Boolean = !accessToken().isNullOrBlank()

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
    }
}
