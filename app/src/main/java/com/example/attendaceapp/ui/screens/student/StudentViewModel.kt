package com.example.attendaceapp.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.repository.FirebaseRepository
import com.example.attendaceapp.ui.state.StudentUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    fun recordAttendance(
        sessionId: String,
        studentNIM: String,
        studentName: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.recordAttendance(
                sessionId = sessionId,
                studentNIM = studentNIM,
                studentName = studentName,
            ).fold(
                onSuccess = { record ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            attendanceHistory = currentState.attendanceHistory + record,
                            lastRecordedAttendance = record,
                            successMessage = "Presensi berhasil direkam"
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastRecordedAttendance = null,
                            error = exception.message ?: "Gagal mencatat presensi"
                        )
                    }
                }
            )
        }
    }

    fun loadAttendanceHistory(studentNIM: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val records = repository.getStudentAttendanceHistory(studentNIM)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    attendanceHistory = records
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null, lastRecordedAttendance = null) }
    }
}