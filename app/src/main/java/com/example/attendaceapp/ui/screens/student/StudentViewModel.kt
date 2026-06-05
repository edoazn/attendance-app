package com.example.attendaceapp.ui.screens.student

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.remote.response.ScheduleDto
import com.example.attendaceapp.data.repository.ApiRepository
import com.example.attendaceapp.ui.state.AttendanceTab
import com.example.attendaceapp.ui.state.StudentUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentViewModel(private val repository: ApiRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    // ─── Jadwal ────────────────────────────────────────────────────────────────

    /** Muat jadwal hari ini dari API. */
    fun loadTodaySchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getTodaySchedules()
                .onSuccess { schedules ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            todaySchedules = schedules,
                            // Auto-select jadwal pertama yang aktif
                            selectedSchedule = schedules.firstOrNull { s -> s.isActive == true }
                                ?: schedules.firstOrNull()
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /** Pilih jadwal secara manual (misalnya dari dropdown). */
    fun selectSchedule(schedule: ScheduleDto) {
        _uiState.update { it.copy(selectedSchedule = schedule) }
    }

    /** Ganti tab aktif (QR Scan atau Kode Manual). */
    fun switchTab(tab: AttendanceTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ─── Submit Absensi ────────────────────────────────────────────────────────

    /**
     * Submit absensi via QR Code.
     * [qrToken] adalah UUID yang di-decode dari QR image yang ditampilkan admin.
     */
    fun submitQrAttendance(qrToken: String) {
        val scheduleId = _uiState.value.selectedSchedule?.id ?: run {
            _uiState.update { it.copy(error = "Tidak ada jadwal yang dipilih") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.submitAttendanceQr(scheduleId, qrToken)
                .onSuccess { data ->
                    val status = data.status ?: "hadir"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastAttendanceData = data,
                            successMessage = "✅ Absensi berhasil! Status: $status"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * Submit absensi via kode manual 6-karakter yang diketik mahasiswa.
     */
    fun submitCodeAttendance(code: String) {
        if (code.length != 6) {
            _uiState.update { it.copy(error = "Kode harus 6 karakter") }
            return
        }
        val scheduleId = _uiState.value.selectedSchedule?.id ?: run {
            _uiState.update { it.copy(error = "Tidak ada jadwal yang dipilih") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.submitAttendanceCode(scheduleId, code.uppercase())
                .onSuccess { data ->
                    val status = data.status ?: "hadir"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastAttendanceData = data,
                            successMessage = "✅ Absensi berhasil! Status: $status"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    // ─── Backward-compat ───────────────────────────────────────────────────────

    @Deprecated("Gunakan loadTodaySchedules()")
    fun loadAttendanceHistory(studentNIM: String) {
        loadTodaySchedules()
    }

    @Deprecated("Gunakan submitQrAttendance(qrToken)")
    fun recordAttendance(sessionId: String, studentNIM: String, studentName: String) {
        submitQrAttendance(sessionId)
    }

    // ─── Utilities ─────────────────────────────────────────────────────────────

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                successMessage = null,
                lastAttendanceData = null,
                lastRecordedAttendance = null
            )
        }
    }

    // ─── Factory ───────────────────────────────────────────────────────────────

    companion object {
        /** Factory yang menyuntikkan Context ke ApiRepository. */
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    StudentViewModel(ApiRepository(context = context)) as T
            }
    }
}