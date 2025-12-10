package com.example.attendaceapp.ui.state

import com.example.attendaceapp.data.model.AttendanceSession

data class LecturerUiState(
    val isLoading: Boolean = false,
    val activeSessions: List<AttendanceSession> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)
