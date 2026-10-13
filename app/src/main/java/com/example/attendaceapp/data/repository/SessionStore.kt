package com.example.attendaceapp.data.repository

import android.content.Context
import com.example.attendaceapp.data.local.UserPreferences
import com.example.attendaceapp.data.model.User

/**
 * Abstraksi penyimpanan sesi agar [SupabaseRepository] bisa di-unit-test
 * tanpa Android framework (pakai fake di test, [UserPreferencesSessionStore]
 * di production).
 */
interface SessionStore {
    /** Token sesi yang tersimpan, atau null bila belum login. */
    fun getToken(): String?

    /** Simpan token + data user (dipanggil setelah login sukses). */
    fun save(token: String, user: User)

    /** Hapus seluruh sesi lokal (logout). */
    fun clear()
}

/**
 * Implementasi [SessionStore] di atas SharedPreferences via [UserPreferences].
 * dipakai default oleh [SupabaseRepository] bila context tersedia.
 */
class UserPreferencesSessionStore(context: Context) : SessionStore {
    private val prefs = UserPreferences.getInstance(context)

    override fun getToken(): String? = prefs.authToken

    override fun save(token: String, user: User) {
        prefs.authToken = token
        prefs.userId = user.id
        prefs.userName = user.name
        prefs.userNim = user.nim
        prefs.userEmail = user.email
        prefs.userRole = user.role.name
    }

    override fun clear() = prefs.clear()
}
