package com.example.attendaceapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class UserPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }


    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit { putString(KEY_USER_ID, value) }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var userNim: String?
        get() = prefs.getString(KEY_USER_NIM, null)
        set(value) = prefs.edit { putString(KEY_USER_NIM, value) }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_USER_EMAIL, value) }

    var userRole: String?
        get() = prefs.getString(KEY_USER_ROLE, null)
        set(value) = prefs.edit { putString(KEY_USER_ROLE, value) }

    /** Apakah user sudah login (token tersedia). */
    val isLoggedIn: Boolean
        get() = authToken != null

    /** Hapus semua data sesi (logout). */
    fun clear() = prefs.edit { clear() }

    companion object {
        private const val PREF_NAME    = "user_prefs"
        private const val KEY_TOKEN    = "auth_token"
        private const val KEY_USER_ID  = "user_id"
        private const val KEY_USER_NAME  = "user_name"
        private const val KEY_USER_NIM   = "user_nim"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE  = "user_role"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
