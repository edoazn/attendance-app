package com.example.attendaceapp.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.local.UserPreferences
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.model.UserRole
import com.example.attendaceapp.data.repository.ApiRepository
import com.example.attendaceapp.ui.state.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel untuk autentikasi.
 * Extend [AndroidViewModel] agar bisa akses [Application] context
 * untuk menyimpan token ke SharedPreferences via [ApiRepository].
 *
 * Pada saat init, sesi yang tersimpan otomatis di-restore sehingga
 * user tidak perlu login ulang setiap buka app.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ApiRepository(context = application)
    private val prefs = UserPreferences.getInstance(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        restoreSession()
    }

    /**
     * Baca sesi login yang tersimpan (token + data user) dari SharedPreferences.
     * Dipanggil satu kali saat ViewModel pertama dibuat.
     * Jika token ada, langsung set state ke [AuthState.Success] tanpa perlu hit API.
     */
    private fun restoreSession() {
        if (prefs.isLoggedIn) {
            val user = User(
                id    = prefs.userId    ?: "",
                nim   = prefs.userNim   ?: "",
                name  = prefs.userName  ?: "",
                email = prefs.userEmail ?: "",
                role  = when (prefs.userRole) {
                    "LECTURER" -> UserRole.LECTURER
                    else       -> UserRole.STUDENT
                }
            )
            _currentUser.value = user
            _authState.value = AuthState.Success(user)
        }
        // Jika tidak ada token, state tetap Idle → tampilkan Login
    }

    fun login(nim: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            repository.login(nim, password).fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Terjadi kesalahan saat login"
                    )
                }
            )
        }
    }

    fun logout() {
        repository.clearSession()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}