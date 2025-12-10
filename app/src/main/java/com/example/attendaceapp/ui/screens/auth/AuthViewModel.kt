package com.example.attendaceapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.repository.FirebaseRepository
import com.example.attendaceapp.ui.state.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

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
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}