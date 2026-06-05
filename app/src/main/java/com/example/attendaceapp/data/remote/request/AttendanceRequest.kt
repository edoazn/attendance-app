package com.example.attendaceapp.data.remote.request

import com.google.gson.annotations.SerializedName

data class AttendanceRequest(
    @field:SerializedName("schedule_id")
    val scheduleId: Int,

    @field:SerializedName("method")
    val method: String,

    // Geolocation
    @field:SerializedName("latitude")
    val latitude: Double? = null,

    @field:SerializedName("longitude")
    val longitude: Double? = null,

    // QR Code scan
    @field:SerializedName("qr_token")
    val qrToken: String? = null,

    // Input kode manual 6-karakter
    @field:SerializedName("attendance_code")
    val attendanceCode: String? = null,
) {
    companion object {
        const val METHOD_GEOLOCATION = "geolocation"
        const val METHOD_QR_CODE = "qr_code"
        const val METHOD_ATTENDANCE_CODE = "attendance_code"
    }
}