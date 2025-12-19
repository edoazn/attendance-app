package com.example.attendaceapp.ui.state

import com.example.attendaceapp.data.model.AttendanceRecord

data class StudentUiState(
    val isLoading: Boolean = false,
    val attendanceHistory: List<AttendanceRecord> = emptyList(),
    val lastRecordedAttendance: AttendanceRecord? = null,
    val error: String? = null,
    val successMessage: String? = null
)
