package com.example.caredose

import android.content.SharedPreferences
import android.content.Context
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_NAME = "user_session"
        private const val KEY_USER_ID = "userId"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    }


    fun saveSession(userId: Long) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, userId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }


    fun getUserId(): Long? {
        return if (isLoggedIn()) {
            prefs.getLong(KEY_USER_ID, -1).takeIf { it != -1L }
        } else {
            null
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}