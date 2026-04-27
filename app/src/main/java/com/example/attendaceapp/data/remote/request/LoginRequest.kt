package com.example.attendaceapp.data.remote.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @field:SerializedName("identity_number")
    val identityNumber: String,

    @field:SerializedName("password")
    val password: String,
)