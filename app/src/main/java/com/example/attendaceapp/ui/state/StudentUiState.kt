package com.example.attendaceapp.ui.state

import com.example.attendaceapp.data.remote.response.AttendanceData
import com.example.attendaceapp.data.remote.response.ScheduleDto

/** Tab yang aktif di AttendancePage */
enum class AttendanceTab { QR_SCAN, MANUAL_CODE }

data class StudentUiState(
    // ─── Loading / Error umum ──────────────────────────────────────────────────
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // ─── Jadwal hari ini ───────────────────────────────────────────────────────
    val todaySchedules: List<ScheduleDto> = emptyList(),
    val selectedSchedule: ScheduleDto? = null,

    // ─── Absensi ───────────────────────────────────────────────────────────────
    /** Tab yang sedang aktif di layar AttendancePage */
    val activeTab: AttendanceTab = AttendanceTab.QR_SCAN,

    /** Data absensi terakhir yang berhasil dicatat */
    val lastAttendanceData: AttendanceData? = null,

    // ─── Backward-compat ───────────────────────────────────────────────────────
    @Deprecated("Gunakan todaySchedules atau lastAttendanceData")
    val attendanceHistory: List<com.example.attendaceapp.data.model.AttendanceRecord> = emptyList(),

    @Deprecated("Gunakan lastAttendanceData")
    val lastRecordedAttendance: com.example.attendaceapp.data.model.AttendanceRecord? = null
)
