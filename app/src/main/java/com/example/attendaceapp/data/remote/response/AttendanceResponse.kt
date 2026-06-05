package com.example.attendaceapp.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Envelope utama dari POST /attendance.
 * Struktur: { "success": true, "message": "...", "data": { ... } }
 */
data class AttendanceApiResponse(
    @field:SerializedName("success")
    val success: Boolean? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("data")
    val data: AttendanceData? = null
)

data class AttendanceData(
    @field:SerializedName("status")
    val status: String? = null,

    @field:SerializedName("distance")
    val distance: Double? = null,

    @field:SerializedName("method")
    val method: String? = null,

    @field:SerializedName("attendance")
    val attendance: AttendanceDetail? = null
)

data class AttendanceDetail(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("status")
    val status: String? = null,

    @field:SerializedName("method")
    val method: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null
)

// ─── Schedule Response ────────────────────────────────────────────────────────

/**
 * Envelope untuk GET /schedules/today.
 */
data class ScheduleListResponse(
    @field:SerializedName("success")
    val success: Boolean? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("data")
    val data: List<ScheduleDto>? = null
)

data class ScheduleDto(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("course_name")
    val courseName: String? = null,

    @field:SerializedName("lecturer_name")
    val lecturerName: String? = null,

    @field:SerializedName("room")
    val room: String? = null,

    /** Format "HH:mm" atau "HH:mm:ss" dari API */
    @field:SerializedName("start_time")
    val startTime: String? = null,

    /** Format "HH:mm" atau "HH:mm:ss" dari API */
    @field:SerializedName("end_time")
    val endTime: String? = null,

    /** Nama hari dalam bahasa Inggris: "Monday", "Tuesday", dst. */
    @field:SerializedName("day")
    val day: String? = null,

    @field:SerializedName("is_active")
    val isActive: Boolean? = null,

    @field:SerializedName("has_qr")
    val hasQr: Boolean? = null,

    @field:SerializedName("has_active_code")
    val hasActiveCode: Boolean? = null
) {
    /** Ambil "HH:mm" saja dari string start_time (API kadang kirim "HH:mm:ss") */
    fun startTimeShort(): String = startTime?.take(5) ?: "—"
    fun endTimeShort(): String   = endTime?.take(5) ?: "—"
}

// Backward-compat alias — dipertahankan agar tidak breaking import lama
@Deprecated("Gunakan AttendanceData", replaceWith = ReplaceWith("AttendanceData"))
typealias AttendanceResponse = AttendanceData
