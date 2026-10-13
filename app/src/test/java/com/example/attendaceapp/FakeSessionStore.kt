package com.example.attendaceapp

import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.repository.SessionStore

/**
 * Fake [SessionStore] bersama untuk unit test (tanpa backend asli).
 * Diekstrak dari duplikat di HistoryProfileTest/SupabaseRepositoryTest
 * agar satu definisi dipakai semua test repository.
 */
class FakeSessionStore : SessionStore {
    @JvmField var token: String? = null
    var user: User? = null

    override fun getToken(): String? = token

    override fun save(token: String, user: User) {
        this.token = token
        this.user = user
    }

    override fun clear() {
        token = null
        user = null
    }
}
