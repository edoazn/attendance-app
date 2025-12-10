package com.example.attendaceapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttendanceRecord(
    val id: String = "",
    val sessionId: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val studentNIM: String = "",
    val studentName: String = "",
    val lecturerId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val scannedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    EXCUSED
}