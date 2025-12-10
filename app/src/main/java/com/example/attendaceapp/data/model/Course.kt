package com.example.attendaceapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Course(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val lecturerId: String = "",
    val lecturerName: String = "",
    val semester: String = "",
    val enrolledStudents: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) : Parcelable