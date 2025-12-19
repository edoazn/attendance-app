package com.example.attendaceapp.ui.screens.lecturer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.model.AttendanceSession
import com.example.attendaceapp.data.repository.FirebaseRepository
import com.example.attendaceapp.ui.state.LecturerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LecturerViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _uiState = MutableStateFlow(LecturerUiState())
    val uiState: StateFlow<LecturerUiState> = _uiState.asStateFlow()

    fun createStudent(
        nim: String,
        name: String,
        email: String,
        department: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.createStudent(nim, name, email, department, password).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Berhasil menambahkan mahasiswa"
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                                ?: "Terjadi kesalahan saat menambahkan mahasiswa"
                        )
                    }
                }
            )
        }
    }

    fun createAttendanceSession(
        courseId: String,
        courseName: String,
        lecturerId: String,
        lecturerName: String,
        durationInMinutes: Int,
        lateThreshold: Int = 15
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val session = AttendanceSession(
                id = UUID.randomUUID().toString(),
                courseId = courseId,
                courseName = courseName,
                lecturerId = lecturerId,
                lecturerName = lecturerName,
                qrCode = UUID.randomUUID().toString(),
                sessionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date()
                ),
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (durationInMinutes * 60 * 1000),
                isActive = true,
                attendanceCount = 0,
                lateThreshold = 15
            )

            repository.createAttendanceSession(session).fold(
                onSuccess = { createSession ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            activeSessions = currentState.activeSessions + session,
                            successMessage = "Sesi presensi berhasil dibuat"
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Gagal membuat sesi presensi"
                        )
                    }
                }
            )
        }
    }

    fun loadActiveSessions(lecturerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }


            // TODO: Implement getActiveSessions in repository
            // For now, just reset loading state
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}