package com.example.attendaceapp.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @field:SerializedName("id")
    val id: String = "",

    @field:SerializedName("identity_number")
    val nim: String = "",

    @field:SerializedName("name")
    val name: String = "",

    @field:SerializedName("email")
    val email: String = "",

    @field:SerializedName("role")
    val role: UserRole = UserRole.STUDENT,

    // Dipertahankan untuk kompatibilitas alur lama saat migrasi backend.
    val department: String = "",
    val passwordHash: String = "",
    val isActive: Boolean = true
) : Parcelable {
    @IgnoredOnParcel
    val identityNumber: String
        get() = nim
}

enum class UserRole {
    STUDENT,
    LECTURER
}
