package com.example.attendaceapp.ui.state

import com.example.attendaceapp.data.model.User

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val error: String) : AuthState()
}

