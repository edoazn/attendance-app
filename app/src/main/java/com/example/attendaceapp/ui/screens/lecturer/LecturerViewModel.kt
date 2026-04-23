package com.example.attendaceapp.ui.screens.lecturer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.model.AttendanceSession
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.repository.ApiRepository
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

    private val repository = ApiRepository()

    private val _uiState = MutableStateFlow(LecturerUiState())
    val uiState: StateFlow<LecturerUiState> = _uiState.asStateFlow()

    // Current logged in lecturer
    private var currentLecturer: User? = null

    fun setCurrentUser(user: User){
        currentLecturer = user
        if (user.id.isNotEmpty()){
            loadActiveSessions(user.id)
        }
    }

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
        courseName: String,
        description: String = "",
        durationInMinutes: Int = 60,
        lateThreshold: Int = 15
    ) {
        viewModelScope.launch {
            if (currentLecturer == null){
                _uiState.update { it.copy(error = "User tidak ditemukan") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            val session = AttendanceSession(
                id = "",
                courseId = UUID.randomUUID().toString(),
                courseName = courseName,
                lecturerId = currentLecturer!!.id,
                lecturerName = currentLecturer!!.name,
                qrCode = UUID.randomUUID().toString(),
                sessionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date()
                ),
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (durationInMinutes * 60 * 1000L),
                isActive = true,
                attendanceCount = 0,
                lateThreshold = lateThreshold,
                description = description
            )

            repository.createAttendanceSession(session).fold(
                onSuccess = { sessionId ->
                    val createdSession = session.copy(id = sessionId)
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

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Remove from local state
            _uiState.update { currentState ->
                currentState.copy(
                    activeSessions = currentState.activeSessions.filterNot { it.id == sessionId },
                    isLoading = false,
                )
            }
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