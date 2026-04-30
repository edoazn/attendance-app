package com.example.attendaceapp.data.remote.request

import com.google.gson.annotations.SerializedName

data class AttendanceRequest(
    @field:SerializedName("shedule_id")
    val scheduleId: Int,
    @field:SerializedName("latitude")
    val latitude: Double,
    @field:SerializedName("longitude")
    val longitude: Double,
)