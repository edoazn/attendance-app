package com.example.attendaceapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttendanceSession(
    val id: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val lecturerId: String = "",
    val lecturerName: String = "",
    val qrCode: String = "",
    val sessionDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val attendanceCount: Int = 0,
    val lateThreshold: Int = 15 // in minutes
) : Parcelable