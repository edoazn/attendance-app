package com.example.attendaceapp.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.repository.ApiRepository
import com.example.attendaceapp.ui.state.StudentUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentViewModel : ViewModel() {
    private val repository = ApiRepository()

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    fun recordAttendance(
        sessionId: String,
        studentNIM: String,
        studentName: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
    }

    fun loadAttendanceHistory(studentNIM: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null, lastRecordedAttendance = null) }
    }
}