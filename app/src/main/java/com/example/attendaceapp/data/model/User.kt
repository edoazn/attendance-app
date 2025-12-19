package com.example.attendaceapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val nim: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val passwordHash: String = "",
    val department: String? = "",
    val photoUrl: String? = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val lastLogin: Long = System.currentTimeMillis()
) : Parcelable

enum class UserRole {
    STUDENT,
    LECTURER
}